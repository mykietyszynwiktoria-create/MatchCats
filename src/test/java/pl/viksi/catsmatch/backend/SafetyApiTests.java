package pl.viksi.catsmatch.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pl.viksi.catsmatch.backend.account.*;
import pl.viksi.catsmatch.backend.cats.*;
import pl.viksi.catsmatch.backend.chat.ChatService;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="app.moderation.account-ids=2000000000")
@AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class SafetyApiTests {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired AccountService accounts;
    @Autowired CatService cats; @Autowired ChatService chats; @Autowired JdbcTemplate db;
    @Autowired PasswordEncoder encoder;
    @Autowired pl.viksi.catsmatch.backend.safety.SafetyRetention retention;
    Authentication auth(String name){return UsernamePasswordAuthenticationToken.authenticated(name,null,List.of());}
    int breeder(String name){
        int id=accounts.register(new AccountService.Registration(name,"StrongTestPassword!",name+"@example.test","Test","Breeder")).id();
        cats.saveBreeder(auth(name),new CatService.BreederInput(name+" Cats","Warsaw","Poland",""));return id;
    }
    int cat(String name,Cat.Sex sex){return cats.create(auth(name),new CatService.CatInput(name+" Cat","Maine Coon",sex,Cat.Health.HEALTHY,LocalDate.of(2022,1,1),"Warsaw","Poland","Profile description",true)).id();}
    void moderator(){db.update("INSERT INTO mc_accounts(id,username,password_hash,email,first_name,surname) VALUES (2000000000,'moderator',?,'moderator@example.test','Test','Moderator')",encoder.encode("StrongTestPassword!"));}
    String report(String type,long id){try{return json.writeValueAsString(Map.of("targetType",type,"targetId",id,"reason","HARASSMENT","details","Please review this reported content."));}catch(Exception e){throw new RuntimeException(e);}}
    long submit(String name,String type,long target) throws Exception {
        var response=mvc.perform(post("/safety/reports").with(user(name)).with(csrf()).contentType("application/json").content(report(type,target)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.snapshot").isEmpty()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }
    @Test void blocksStopBothDirectionsIncludingSavedPairAndCanBeRemoved() throws Exception {
        int alice=breeder("alice"),bob=breeder("bob");int a=cat("alice",Cat.Sex.FEMALE),b=cat("bob",Cat.Sex.MALE);
        long chat=chats.contact(b,auth("alice")).id();
        mvc.perform(post("/cats/"+a+"/matches").with(user("alice")).with(csrf()).contentType("application/json").content("{\"candidateId\":"+b+"}")).andExpect(status().isOk());
        mvc.perform(put("/safety/blocks/"+bob).with(user("alice")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(put("/safety/blocks/"+bob).with(user("alice")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/safety/blocks").with(user("alice"))).andExpect(jsonPath("$.length()").value(1));
        for(String name:List.of("alice","bob")){
            mvc.perform(post("/chats/"+chat+"/messages").with(user(name)).with(csrf()).contentType("application/json").content("{\"text\":\"Not allowed\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CONTACT_BLOCKED"));
            mvc.perform(get("/chats/"+chat+"/messages").with(user(name))).andExpect(status().isOk());
            mvc.perform(get("/chats/"+chat).with(user(name))).andExpect(jsonPath("$.contactBlocked").value(true));
        }
        mvc.perform(post("/cats/"+b+"/contact").with(user("alice")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/cats/"+a+"/contact").with(user("bob")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/cats/"+a+"/candidates").with(user("alice"))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(post("/cats/"+a+"/matches").with(user("alice")).with(csrf()).contentType("application/json").content("{\"candidateId\":"+b+"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/safety/blocks/"+alice).with(user("bob")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/chats/"+chat).with(user("bob"))).andExpect(jsonPath("$.contactBlocked").value(true));
        mvc.perform(delete("/safety/blocks/"+bob).with(user("alice")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(post("/chats/"+chat+"/messages").with(user("bob")).with(csrf()).contentType("application/json").content("{\"text\":\"Allowed again\"}")).andExpect(status().isCreated());
    }
    @Test void blocksRequireAuthenticationCsrfAndAnotherExistingAccount() throws Exception {
        int alice=breeder("alice");int bob=breeder("bob");
        mvc.perform(get("/safety/blocks")).andExpect(status().isUnauthorized());
        mvc.perform(put("/safety/blocks/"+bob).with(user("alice"))).andExpect(status().isForbidden());
        mvc.perform(put("/safety/blocks/"+alice).with(user("alice")).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(put("/safety/blocks/999999999").with(user("alice")).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/moderation/reports").with(user("alice").roles("ADMIN"))).andExpect(status().isForbidden());
    }
    @Test void reportsOnlyExposeOwnStatusAndOnlyModeratorSeesEvidence() throws Exception {
        breeder("alice");breeder("bob");breeder("eve");moderator();int b=cat("bob",Cat.Sex.MALE);
        long chat=chats.contact(b,auth("alice")).id();long message=chats.send(chat,auth("bob"),new ChatService.MessageInput("Offending message evidence")).id();
        long id=submit("alice","MESSAGE",message);
        mvc.perform(get("/safety/reports").with(user("alice"))).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].snapshot").isEmpty()).andExpect(jsonPath("$.items[0].decisionNote").isEmpty());
        mvc.perform(get("/safety/reports").with(user("eve"))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(post("/safety/reports").with(user("eve")).with(csrf()).contentType("application/json").content(report("MESSAGE",message))).andExpect(status().isForbidden());
        mvc.perform(post("/safety/reports").with(user("bob")).with(csrf()).contentType("application/json").content(report("MESSAGE",message))).andExpect(status().isBadRequest());
        mvc.perform(get("/moderation/reports").with(user("moderator"))).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].snapshot").value("Offending message evidence"));
        mvc.perform(post("/moderation/reports/"+id+"/decision").with(user("moderator")).with(csrf()).contentType("application/json").content("{\"status\":\"DISMISSED\",\"action\":\"NONE\",\"note\":\"No violation found\"}")).andExpect(status().isNoContent());
        mvc.perform(get("/safety/reports").with(user("alice"))).andExpect(jsonPath("$.items[0].status").value("DISMISSED"));
        mvc.perform(post("/moderation/reports/"+id+"/decision").with(user("moderator")).with(csrf()).contentType("application/json").content("{\"status\":\"RESOLVED\",\"action\":\"NONE\",\"note\":\"Second review\"}")).andExpect(status().isConflict());
    }
    @Test void suspensionHidesProfilesRejectsLoginAndCanBeReversed() throws Exception {
        breeder("alice");int bob=breeder("bob");moderator();int b=cat("bob",Cat.Sex.MALE);long report=submit("alice","CAT",b);
        mvc.perform(post("/moderation/reports/"+report+"/decision").with(user("moderator")).with(csrf()).contentType("application/json").content("{\"status\":\"RESOLVED\",\"action\":\"SUSPEND_ACCOUNT\",\"note\":\"Repeated harassment\"}")).andExpect(status().isNoContent());
        mvc.perform(get("/users/me").with(user("bob"))).andExpect(status().isForbidden());
        mvc.perform(post("/auth/login").with(csrf()).contentType("application/json").content("{\"username\":\"bob\",\"password\":\"StrongTestPassword!\"}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/cats").with(user("alice"))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/cats/"+b).with(user("alice"))).andExpect(status().isNotFound());
        mvc.perform(get("/moderation/suspended").with(user("moderator"))).andExpect(jsonPath("$[0].accountId").value(bob));
        mvc.perform(post("/moderation/accounts/"+bob+"/reinstate").with(user("alice")).with(csrf()).contentType("application/json").content("{\"note\":\"Appeal accepted\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/moderation/accounts/"+bob+"/reinstate").with(user("moderator")).with(csrf()).contentType("application/json").content("{\"note\":\"Appeal accepted\"}")).andExpect(status().isNoContent());
        assertEquals(2,db.queryForObject("SELECT count(*) FROM mc_moderation_actions WHERE account_id=?",Integer.class,bob));
        mvc.perform(get("/cats/"+b).with(user("alice"))).andExpect(status().isOk());
        mvc.perform(get("/users/me").with(user("bob"))).andExpect(status().isOk());
    }
    @Test void malformedReportsAndDailyFloodAreRejected() throws Exception {
        breeder("alice");breeder("bob");int b=cat("bob",Cat.Sex.MALE);
        mvc.perform(post("/safety/reports").with(user("alice")).contentType("application/json").content(report("CAT",b))).andExpect(status().isForbidden());
        mvc.perform(post("/safety/reports").with(user("alice")).with(csrf()).contentType("application/json").content("{\"targetType\":\"CAT\",\"targetId\":"+b+",\"reason\":\"OTHER\",\"details\":\"          \"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/safety/reports").with(user("alice")).with(csrf()).contentType("application/json").content(report("CAT",Long.MAX_VALUE))).andExpect(status().isNotFound());
        for(int i=0;i<20;i++)submit("alice","CAT",b);
        mvc.perform(post("/safety/reports").with(user("alice")).with(csrf()).contentType("application/json").content(report("CAT",b))).andExpect(status().isTooManyRequests());
        assertEquals(20,db.queryForObject("SELECT count(*) FROM mc_safety_reports",Integer.class));
    }
    @Test void oldEvidenceIsPurgedAndFreshEvidenceSurvivesAccountDeletion() throws Exception {
        breeder("alice");int bob=breeder("bob");moderator();int b=cat("bob",Cat.Sex.MALE);
        long fresh=submit("alice","CAT",b),old=submit("alice","CAT",b);
        db.update("UPDATE mc_safety_reports SET created_at=CURRENT_TIMESTAMP-INTERVAL '91 days' WHERE id=?",old);
        db.update("DELETE FROM mc_accounts WHERE id=?",bob);
        retention.purge();
        assertEquals(1,db.queryForObject("SELECT count(*) FROM mc_safety_reports",Integer.class));
        mvc.perform(get("/moderation/reports").with(user("moderator"))).andExpect(jsonPath("$.items[0].id").value(fresh))
            .andExpect(jsonPath("$.items[0].reportedAccountId").isEmpty()).andExpect(jsonPath("$.items[0].snapshot").value("bob Cat\nMaine Coon\nProfile description"));
    }
}
