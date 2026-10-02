package pl.viksi.catsmatch.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pl.viksi.catsmatch.backend.account.AccountService;
import pl.viksi.catsmatch.backend.cats.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class MatchingApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AccountService accounts;
    @Autowired CatService cats;
    @Autowired EntityManager entities;

    Authentication breeder(String username) {
        accounts.register(new AccountService.Registration(username, "StrongTestPassword!",
            username + "@example.test", "Test", "Breeder"));
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(username, null, List.of());
        cats.saveBreeder(auth, new CatService.BreederInput("Blue Cats", "Warsaw", "Poland", ""));
        return auth;
    }

    int cat(Authentication owner, String breed, Cat.Sex sex, boolean available) {
        return cats.create(owner, new CatService.CatInput("Luna", breed, sex, Cat.Health.HEALTHY,
            LocalDate.of(2022, 1, 1), "Warsaw", "Poland", "", available)).id();
    }

    long select(int source, int candidate, String username) throws Exception {
        String response = mvc.perform(post("/cats/" + source + "/matches").with(user(username)).with(csrf())
            .contentType("application/json").content(json.writeValueAsString(Map.of("candidateId", candidate))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    @Test void candidatesExcludeSameOwnerSexBreedAndUnavailableCats() throws Exception {
        var alice = breeder("alice"); var bob = breeder("bob");
        int source = cat(alice, "Maine Coon", Cat.Sex.FEMALE, true);
        int eligible = cat(bob, "maine coon", Cat.Sex.MALE, true);
        cat(alice, "Maine Coon", Cat.Sex.MALE, true);
        cat(bob, "Ragdoll", Cat.Sex.MALE, true);
        cat(bob, "Maine Coon", Cat.Sex.FEMALE, true);
        cat(bob, "Maine Coon", Cat.Sex.MALE, false);
        mvc.perform(get("/cats/" + source + "/candidates").with(user("alice")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].id").value(eligible));
        mvc.perform(get("/cats/" + source + "/candidates").with(user("bob"))).andExpect(status().isForbidden());
        mvc.perform(get("/cats/" + source + "/candidates").with(user("alice")).param("size", "101"))
            .andExpect(status().isBadRequest());
    }

    @Test void selectionsPersistReuseReversePairAndOpenParticipantOnlyChat() throws Exception {
        var alice = breeder("alice"); var bob = breeder("bob"); breeder("eve");
        int source = cat(alice, "Maine Coon", Cat.Sex.FEMALE, true);
        int candidate = cat(bob, "Maine Coon", Cat.Sex.MALE, true);
        long pair = select(source, candidate, "alice");
        assertEquals(pair, select(source, candidate, "alice"));
        assertEquals(pair, select(candidate, source, "bob"));
        entities.clear();
        String list = mvc.perform(get("/cats/" + source + "/matches").with(user("alice")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
            .andReturn().getResponse().getContentAsString();
        long chat = json.readTree(list).get("items").get(0).get("conversationId").asLong();
        mvc.perform(get("/chats/" + chat).with(user("bob"))).andExpect(status().isOk());
        mvc.perform(get("/chats/" + chat).with(user("eve"))).andExpect(status().isForbidden());
        mvc.perform(get("/cats/" + source + "/matches").with(user("eve"))).andExpect(status().isForbidden());
        mvc.perform(delete("/cats/" + candidate).with(user("bob")).with(csrf())).andExpect(status().isNoContent());
        entities.clear();
        mvc.perform(get("/cats/" + source + "/matches").with(user("alice")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/chats/" + chat).with(user("alice"))).andExpect(status().isOk());
    }

    @Test void invalidSelectionsCannotBypassFiltersOrOwnership() throws Exception {
        var alice = breeder("alice"); var bob = breeder("bob");
        int source = cat(alice, "Maine Coon", Cat.Sex.FEMALE, true);
        int eligible = cat(bob, "Maine Coon", Cat.Sex.MALE, true);
        int[] invalid = {source, cat(alice, "Maine Coon", Cat.Sex.MALE, true),
            cat(bob, "Ragdoll", Cat.Sex.MALE, true), cat(bob, "Maine Coon", Cat.Sex.FEMALE, true),
            cat(bob, "Maine Coon", Cat.Sex.MALE, false), -1};
        for (int candidate : invalid) {
            mvc.perform(post("/cats/" + source + "/matches").with(user("alice")).with(csrf())
                .contentType("application/json").content(json.writeValueAsString(Map.of("candidateId", candidate))))
                .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/cats/" + source + "/matches").with(user("bob")).with(csrf())
            .contentType("application/json").content(json.writeValueAsString(Map.of("candidateId", eligible))))
            .andExpect(status().isForbidden());
        mvc.perform(post("/cats/" + source + "/matches").with(user("alice")).with(csrf())
            .contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/cats/" + source + "/matches").with(user("alice")).with(csrf())
            .contentType("application/json").content("{\"candidateId\":999999999}"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/cats/" + source + "/candidates")).andExpect(status().isUnauthorized());
        mvc.perform(post("/cats/" + source + "/matches").with(user("alice"))
            .contentType("application/json").content(json.writeValueAsString(Map.of("candidateId", eligible))))
            .andExpect(status().isForbidden());
        int unavailable = cat(alice, "Maine Coon", Cat.Sex.FEMALE, false);
        mvc.perform(get("/cats/" + unavailable + "/candidates").with(user("alice"))).andExpect(status().isBadRequest());
    }
}
