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

    long send(long chat,String owner) throws Exception {
        var response=mvc.perform(post("/chats/"+chat+"/messages").with(user(owner)).with(csrf()).contentType("application/json").content("{\"text\":\"Unread test\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }
    void read(long chat,String owner,long... ids) throws Exception {
        mvc.perform(post("/chats/"+chat+"/read").with(user(owner)).with(csrf()).contentType("application/json")
            .content(json.writeValueAsString(Map.of("messageIds",ids)))).andExpect(status().isNoContent());
    }

    @Test void unreadCountsOnlyReceivedMessagesAndExplicitReadPersists() throws Exception {
        breeder("alice");breeder("bob");breeder("eve");long chat=contact(cat("bob"),"alice");
        long first=send(chat,"alice"),second=send(chat,"alice");send(chat,"bob");
        mvc.perform(get("/chats/unread").with(user("bob"))).andExpect(jsonPath("$.unreadMessages").value(2)).andExpect(jsonPath("$.unreadConversations").value(1));
        mvc.perform(get("/chats/unread").with(user("alice"))).andExpect(jsonPath("$.unreadMessages").value(1));
        mvc.perform(get("/chats/unread").with(user("eve"))).andExpect(jsonPath("$.unreadMessages").value(0));
        mvc.perform(get("/chats/"+chat+"/messages").with(user("bob")).param("size","1")).andExpect(status().isOk());
        mvc.perform(get("/chats/"+chat).with(user("bob"))).andExpect(jsonPath("$.unreadMessages").value(2));
        read(chat,"bob",first);read(chat,"bob",first);
        entities.flush();entities.clear();
        mvc.perform(get("/chats/"+chat).with(user("bob"))).andExpect(jsonPath("$.unreadMessages").value(1));
        long newer=send(chat,"alice");read(chat,"bob",second);
        mvc.perform(get("/chats/unread").with(user("bob"))).andExpect(jsonPath("$.unreadMessages").value(1));
        read(chat,"bob",newer);
        mvc.perform(get("/chats/unread").with(user("bob"))).andExpect(jsonPath("$.unreadMessages").value(0));
    }

    @Test void newMessagesCreatePrivateNotificationsThatCanBeRead() throws Exception {
        breeder("alice"); breeder("bob"); long chat=contact(cat("bob"),"alice");
        send(chat,"alice");
        mvc.perform(get("/notifications/unread-count").with(user("bob")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        mvc.perform(get("/notifications/unread-count").with(user("alice")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
        String notification=mvc.perform(get("/notifications").with(user("bob")))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].kind").value("MESSAGE"))
            .andExpect(jsonPath("$[0].readAt").value(org.hamcrest.Matchers.nullValue())).andReturn().getResponse().getContentAsString();
        long id=json.readTree(notification).get(0).get("id").asLong();
        mvc.perform(post("/notifications/"+id+"/read").with(user("bob")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/notifications/unread-count").with(user("bob")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
        mvc.perform(get("/notifications").with(user("bob"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].readAt").exists());
        mvc.perform(get("/notifications").with(user("alice"))).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test void markAllNotificationsReadIsScopedToCurrentAccount() throws Exception {
        breeder("alice"); breeder("bob"); long chat=contact(cat("bob"),"alice");
        send(chat,"alice");
        mvc.perform(post("/notifications/read-all").with(user("bob")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/notifications/unread-count").with(user("bob"))).andExpect(jsonPath("$.count").value(0));
        mvc.perform(get("/notifications/unread-count").with(user("alice"))).andExpect(jsonPath("$.count").value(0));
    }

    @Test void notificationEndpointsRequireAuthenticationAndValidateLimit() throws Exception {
        mvc.perform(get("/notifications/unread-count")).andExpect(status().isUnauthorized());
        breeder("alice");
        mvc.perform(get("/notifications?limit=0").with(user("alice"))).andExpect(status().isBadRequest());
        mvc.perform(get("/notifications?limit=101").with(user("alice"))).andExpect(status().isBadRequest());
    }

    @Test void readReceiptsRequireMembershipCsrfAndValidReceivedIds() throws Exception {
        breeder("alice");breeder("bob");breeder("eve");long chat=contact(cat("bob"),"alice");long message=send(chat,"alice");
        long otherChat=contact(cat("eve"),"alice");long otherMessage=send(otherChat,"alice");
        for(String owner:java.util.List.of("eve","alice")) {
            mvc.perform(post("/chats/"+chat+"/read").with(user(owner)).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("messageIds",java.util.List.of(message)))))
                .andExpect(status().is(owner.equals("eve")?403:400));
        }
        mvc.perform(post("/chats/"+chat+"/read").with(user("bob")).contentType("application/json").content("{\"messageIds\":[1]}")) .andExpect(status().isForbidden());
        for(var ids:java.util.List.of(java.util.List.of(message,otherMessage),java.util.List.of(-1L),java.util.List.of(999999999L)))
            mvc.perform(post("/chats/"+chat+"/read").with(user("bob")).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("messageIds",ids)))) .andExpect(status().isBadRequest());
        mvc.perform(get("/chats/"+chat).with(user("bob"))).andExpect(jsonPath("$.unreadMessages").value(1));
        mvc.perform(post("/chats/"+chat+"/read").with(user("bob")).with(csrf()).contentType("application/json").content("{\"messageIds\":[]}")) .andExpect(status().isBadRequest());
        mvc.perform(get("/chats/unread")).andExpect(status().isUnauthorized());
    }

    @Test void accountDeletionRemovesReadReceiptsWithConversation() throws Exception {
        breeder("alice");breeder("bob");long chat=contact(cat("bob"),"alice");long message=send(chat,"alice");read(chat,"bob",message);
        assertEquals(1,((Number)entities.createNativeQuery("select count(*) from mc_message_reads where message_id=:id").setParameter("id",message).getSingleResult()).intValue());
        mvc.perform(delete("/users/me").with(user("alice")).with(csrf()).contentType("application/json").content("{\"currentPassword\":\"StrongTestPassword!\"}")) .andExpect(status().isNoContent());
        assertEquals(0,((Number)entities.createNativeQuery("select count(*) from mc_message_reads where message_id=:id").setParameter("id",message).getSingleResult()).intValue());
        mvc.perform(get("/chats/unread").with(user("bob"))).andExpect(jsonPath("$.unreadMessages").value(0));
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

