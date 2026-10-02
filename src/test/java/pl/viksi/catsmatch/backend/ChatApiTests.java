package pl.viksi.catsmatch.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pl.viksi.catsmatch.backend.account.*;
import pl.viksi.catsmatch.backend.cats.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class ChatApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AccountService accounts;
    @Autowired EntityManager entities;

    int breeder(String name) throws Exception {
        int id = accounts.register(new AccountService.Registration(name, "StrongTestPassword!",
            name + "@example.test", "Test", "Breeder")).id();
        mvc.perform(put("/owners/me").with(user(name)).with(csrf()).contentType("application/json")
            .content("{\"kennel\":\"Blue Cats\",\"city\":\"Warsaw\",\"country\":\"Poland\",\"bio\":\"\"}"))
            .andExpect(status().isOk());
        return id;
    }

    int cat(String owner) throws Exception {
        String response = mvc.perform(post("/cats").with(user(owner)).with(csrf()).contentType("application/json")
            .content("{\"name\":\"Luna\",\"breed\":\"Maine Coon\",\"sex\":\"FEMALE\",\"health\":\"HEALTHY\",\"birthDate\":\"2022-01-01\",\"city\":\"Warsaw\",\"country\":\"Poland\",\"description\":\"\",\"available\":true}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asInt();
    }

    long contact(int catId, String owner) throws Exception {
        String response = mvc.perform(post("/cats/" + catId + "/contact").with(user(owner)).with(csrf()))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    @Test void privateMessagesPersistAndAuthorsCannotBeForged() throws Exception {
        int alice = breeder("alice"); breeder("bob"); breeder("eve");
        int bobCat = cat("bob"); long chat = contact(bobCat, "alice");
        assertEquals(chat, contact(bobCat, "alice"));
        assertEquals(chat, contact(cat("alice"), "bob"));
        String text = "Long message ".repeat(50);
        mvc.perform(post("/chats/" + chat + "/messages").with(user("alice")).with(csrf())
            .contentType("application/json").content(json.writeValueAsString(Map.of("text", text, "authorId", 999))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.authorId").value(alice));
        entities.clear();
        mvc.perform(get("/chats/" + chat + "/messages").with(user("bob")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].text").value(text.strip()));
        mvc.perform(get("/chats").with(user("eve"))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/chats/" + chat).with(user("eve"))).andExpect(status().isForbidden());
        mvc.perform(get("/chats/" + chat + "/messages").with(user("eve"))).andExpect(status().isForbidden());
        mvc.perform(post("/chats/" + chat + "/messages").with(user("eve")).with(csrf())
            .contentType("application/json").content("{\"text\":\"Intrusion\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/cats/" + bobCat).with(user("bob")).with(csrf())).andExpect(status().isNoContent());
        entities.clear();
        mvc.perform(get("/chats/" + chat).with(user("alice"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.contextCatId").doesNotExist());
        mvc.perform(get("/chats/" + chat + "/messages").with(user("alice")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
    }

    @Test void invalidMessagesAndUnauthorizedRequestsAreRejected() throws Exception {
        breeder("alice"); breeder("bob"); int id = cat("bob"); long chat = contact(id, "alice");
        mvc.perform(post("/cats/" + id + "/contact").with(user("bob")).with(csrf())).andExpect(status().isBadRequest());
        for (String text : new String[]{"   ", "x".repeat(4001)}) {
            mvc.perform(post("/chats/" + chat + "/messages").with(user("alice")).with(csrf())
                .contentType("application/json").content(json.writeValueAsString(Map.of("text", text))))
                .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/chats")).andExpect(status().isUnauthorized());
        mvc.perform(post("/chats/" + chat + "/messages").with(user("alice"))
            .contentType("application/json").content("{\"text\":\"Hello\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/chats/999999999/messages").with(user("alice"))).andExpect(status().isNotFound());
        mvc.perform(get("/chats/" + chat + "/messages").with(user("alice")).param("size", "101"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/chats/" + chat + "/messages").with(user("bob")).with(csrf())
            .contentType("application/json").content("{\"text\":\"First\"}"))
            .andExpect(status().isCreated());
        mvc.perform(post("/chats/" + chat + "/messages").with(user("alice")).with(csrf())
            .contentType("application/json").content("{\"text\":\"Second\"}"))
            .andExpect(status().isCreated());
        mvc.perform(get("/chats/" + chat + "/messages").with(user("alice")).param("size", "1").param("page", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.items[0].text").value("Second"));
    }
}
