package pl.viksi.catsmatch.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pl.viksi.catsmatch.backend.account.AccountService;
import pl.viksi.catsmatch.backend.cats.*;
import pl.viksi.catsmatch.backend.documents.*;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="app.moderation.account-ids=2000000000") @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class DocumentApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AccountService accounts;
    @Autowired CatService cats;
    @Autowired DocumentRepository documents;
    @Autowired EntityManager entities;
    @Autowired org.springframework.jdbc.core.JdbcTemplate db;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    final byte[] pdf = "%PDF-1.7\nTest signature fixture".getBytes(StandardCharsets.US_ASCII);

    int fixture() {
        for (String name : new String[]{"alice", "bob", "eve"}) {
            accounts.register(new AccountService.Registration(name, "StrongTestPassword!", name+"@example.test", "Test", "Breeder"));
            if (!name.equals("eve")) {
                cats.saveBreeder(UsernamePasswordAuthenticationToken.authenticated(name, null, List.of()),
                    new CatService.BreederInput("Blue Cats", "Warsaw", "Poland", ""));
            }
        }
        return cats.create(UsernamePasswordAuthenticationToken.authenticated("alice", null, List.of()),
            new CatService.CatInput("Luna", "Maine Coon", Cat.Sex.FEMALE, Cat.Health.HEALTHY,
                LocalDate.of(2022,1,1), "Warsaw", "Poland", "", true)).id();
    }

    long upload(int cat) throws Exception {
        String response = mvc.perform(multipart("/cats/"+cat+"/documents")
            .file(new MockMultipartFile("file", "pedigree.pdf", "text/plain", pdf))
            .param("kind", "PEDIGREE").with(user("alice")).with(csrf()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.visibility").value("PRIVATE"))
            .andExpect(jsonPath("$.mediaType").value("application/pdf"))
            .andExpect(jsonPath("$.verification").value("OWNER_UPLOADED"))
            .andExpect(jsonPath("$.content").doesNotExist()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    @Test void filesArePrivateUntilOwnerSharesAndCanBeRevoked() throws Exception {
        int cat = fixture(); long id = upload(cat); entities.clear();
        mvc.perform(get("/documents/"+id+"/download").with(user("alice")))
            .andExpect(status().isOk()).andExpect(content().bytes(pdf))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"document-"+id+".pdf\""))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/cats/"+cat+"/documents").with(user("bob")))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/documents/"+id+"/download").with(user("bob"))).andExpect(status().isForbidden());
        mvc.perform(put("/documents/"+id+"/visibility").with(user("bob")).with(csrf())
            .contentType("application/json").content("{\"visibility\":\"BREEDERS\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(put("/documents/"+id+"/visibility").with(user("alice")).with(csrf())
            .contentType("application/json").content("{\"visibility\":\"BREEDERS\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/documents/"+id+"/download").with(user("bob")))
            .andExpect(status().isOk()).andExpect(content().bytes(pdf));
        mvc.perform(get("/documents/"+id+"/download").with(user("eve"))).andExpect(status().isNotFound());
        mvc.perform(get("/cats/"+cat+"/documents").with(user("bob"))).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(id)).andExpect(jsonPath("$[0].content").doesNotExist());
        mvc.perform(put("/documents/"+id+"/visibility").with(user("alice")).with(csrf())
            .contentType("application/json").content("{\"visibility\":\"PRIVATE\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/documents/"+id+"/download").with(user("bob"))).andExpect(status().isForbidden());
        mvc.perform(delete("/documents/"+id).with(user("bob")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(delete("/documents/"+id).with(user("alice")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/documents/"+id+"/download").with(user("alice"))).andExpect(status().isNotFound());
    }

    @Test void invalidUploadsAndAccessAreRejected() throws Exception {
        int cat = fixture();
        for (MockMultipartFile file : new MockMultipartFile[]{
            new MockMultipartFile("file","empty.pdf","application/pdf",new byte[0]),
            new MockMultipartFile("file","fake.pdf","application/pdf","<script>bad</script>".getBytes()),
            new MockMultipartFile("file","../pedigree.pdf","application/pdf",pdf),
            new MockMultipartFile("file","bad\r\nname.pdf","application/pdf",pdf)}) {
            mvc.perform(multipart("/cats/"+cat+"/documents").file(file).param("kind","PEDIGREE")
                .with(user("alice")).with(csrf())).andExpect(status().isBadRequest());
        }
        mvc.perform(multipart("/cats/"+cat+"/documents")
            .file(new MockMultipartFile("file","big.pdf","application/pdf",new byte[DocumentService.MAX_BYTES+1]))
            .param("kind","PEDIGREE").with(user("alice")).with(csrf()))
            .andExpect(status().isPayloadTooLarge());
        mvc.perform(multipart("/cats/"+cat+"/documents")
            .file(new MockMultipartFile("file","pedigree.pdf","application/pdf",pdf))
            .param("kind","UNKNOWN").with(user("alice")).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(multipart("/cats/"+cat+"/documents")
            .file(new MockMultipartFile("file","pedigree.pdf","application/pdf",pdf))
            .param("kind","PEDIGREE").with(user("bob")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(multipart("/cats/"+cat+"/documents")
            .file(new MockMultipartFile("file","pedigree.pdf","application/pdf",pdf))
            .param("kind","PEDIGREE").with(user("alice"))).andExpect(status().isForbidden());
        mvc.perform(get("/cats/"+cat+"/documents")).andExpect(status().isUnauthorized());
        assertEquals(0,documents.countByCatId(cat));
    }

    @Test void imageTypesUseContentSignaturesAndSharingRequiresValidInput() throws Exception {
        int cat = fixture();
        byte[][] images = {new byte[]{(byte)137,80,78,71,13,10,26,10,0},
            new byte[]{(byte)255,(byte)216,(byte)255,0}};
        String[] types = {"image/png", "image/jpeg"};
        for (int i=0;i<images.length;i++) {
            mvc.perform(multipart("/cats/"+cat+"/documents")
                .file(new MockMultipartFile("file","scan.bin","application/octet-stream",images[i]))
                .param("kind","GENETIC_TEST").with(user("alice")).with(csrf()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.mediaType").value(types[i]));
        }
        long id = upload(cat);
        mvc.perform(put("/documents/"+id+"/visibility").with(user("alice")).with(csrf())
            .contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/documents/"+id+"/visibility").with(user("alice")).with(csrf())
            .contentType("application/json").content("{\"visibility\":\"PUBLIC\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void quotaAndDeletingCatCleanUpDocuments() throws Exception {
        int cat = fixture();
        for (int i=0;i<DocumentService.MAX_FILES;i++) upload(cat);
        mvc.perform(multipart("/cats/"+cat+"/documents")
            .file(new MockMultipartFile("file","extra.pdf","application/pdf",pdf))
            .param("kind","OTHER").with(user("alice")).with(csrf()))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DOCUMENT_LIMIT"));
        mvc.perform(delete("/cats/"+cat).with(user("alice")).with(csrf())).andExpect(status().isNoContent());
        entities.clear();
        assertEquals(0,documents.countByCatId(cat));
    }

    @Test void ownerCanRequestReviewButUploadIsNotAutomaticallyVerified() throws Exception {
        int cat = fixture(); long id = upload(cat);
        mvc.perform(post("/documents/"+id+"/verification-request").with(user("alice")).with(csrf()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.verification").value("REVIEW_REQUESTED"));
        mvc.perform(post("/documents/"+id+"/verification-request").with(user("alice")).with(csrf()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.verification").value("REVIEW_REQUESTED"));
        mvc.perform(post("/documents/"+id+"/verification-request").with(user("bob")).with(csrf()))
            .andExpect(status().isForbidden());
        mvc.perform(post("/documents/"+id+"/verification-request").with(user("alice")))
            .andExpect(status().isForbidden());
    }

    @Test void moderatorReviewIsPrivateRequiresFreshRequestAndKeepsAudit() throws Exception {
        int cat = fixture(); long id = upload(cat);
        db.update("INSERT INTO mc_accounts(id,username,password_hash,email,first_name,surname) VALUES (2000000000,'moderator',?,'moderator@example.test','Test','Moderator')", encoder.encode("StrongTestPassword!"));
        String download = "/moderation/documents/"+id+"/download";
        String decision = "/moderation/documents/"+id+"/decision";
        mvc.perform(get(download).with(user("moderator"))).andExpect(status().isForbidden());
        String request = mvc.perform(post("/documents/"+id+"/verification-request").with(user("alice")).with(csrf()))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(request).get("verificationRequestedAt").asText();
        mvc.perform(get("/moderation/documents").with(user("alice").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(get("/moderation/documents").with(user("moderator")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].content").doesNotExist());
        mvc.perform(get(download).with(user("moderator"))).andExpect(status().isOk()).andExpect(content().bytes(pdf));
        mvc.perform(get(download).with(user("bob"))).andExpect(status().isForbidden());
        String body = json.writeValueAsString(java.util.Map.of("status","REJECTED","requestedAt",token,"note","Please provide a legible scan."));
        for (String invalid : new String[]{"{}", json.writeValueAsString(java.util.Map.of("status","OWNER_UPLOADED","requestedAt",token,"note","Invalid status")), json.writeValueAsString(java.util.Map.of("status","VERIFIED","requestedAt",token,"note","  "))}) {
            mvc.perform(post(decision).with(user("moderator")).with(csrf()).contentType("application/json").content(invalid)).andExpect(status().isBadRequest());
        }
        mvc.perform(post(decision).with(user("alice")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post(decision).with(user("moderator")).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post(decision).with(user("moderator")).with(csrf()).contentType("application/json").content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.verification").value("REJECTED"));
        mvc.perform(post(decision).with(user("moderator")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
        mvc.perform(get(download).with(user("moderator"))).andExpect(status().isForbidden());
        mvc.perform(put("/documents/"+id+"/visibility").with(user("alice")).with(csrf()).contentType("application/json").content("{\"visibility\":\"BREEDERS\"}"));
        mvc.perform(get("/cats/"+cat+"/documents").with(user("alice"))).andExpect(jsonPath("$[0].verificationNote").value("Please provide a legible scan."));
        mvc.perform(get("/cats/"+cat+"/documents").with(user("bob"))).andExpect(jsonPath("$[0].verificationNote").isEmpty());
        request = mvc.perform(post("/documents/"+id+"/verification-request").with(user("alice")).with(csrf()))
            .andReturn().getResponse().getContentAsString();
        String fresh = json.readTree(request).get("verificationRequestedAt").asText();
        assertNotEquals(token, fresh);
        mvc.perform(post(decision).with(user("moderator")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
        String accepted = json.writeValueAsString(java.util.Map.of("status","VERIFIED","requestedAt",fresh,"note","Checked against the issuing registry."));
        mvc.perform(post(decision).with(user("moderator")).with(csrf()).contentType("application/json").content(accepted))
            .andExpect(status().isOk()).andExpect(jsonPath("$.verification").value("VERIFIED"));
        mvc.perform(post("/documents/"+id+"/verification-request").with(user("alice")).with(csrf())).andExpect(status().isConflict());
        assertEquals(2, db.queryForObject("SELECT count(*) FROM mc_document_reviews WHERE document_id=?", Integer.class, id));
        mvc.perform(delete("/documents/"+id).with(user("alice")).with(csrf())).andExpect(status().isNoContent());
        assertEquals(0, db.queryForObject("SELECT count(*) FROM mc_document_reviews WHERE document_id=?", Integer.class, id));
    }

    @Test void moderatorCannotReviewOwnDocument() throws Exception {
        fixture();
        db.update("INSERT INTO mc_accounts(id,username,password_hash,email,first_name,surname) VALUES (2000000000,'moderator',?,'moderator@example.test','Test','Moderator')", encoder.encode("StrongTestPassword!"));
        var auth = UsernamePasswordAuthenticationToken.authenticated("moderator", null, List.of());
        cats.saveBreeder(auth, new CatService.BreederInput("Moderator Cats", "Warsaw", "Poland", ""));
        int cat = cats.create(auth, new CatService.CatInput("Mia", "Maine Coon", Cat.Sex.FEMALE, Cat.Health.HEALTHY, LocalDate.of(2022,1,1), "Warsaw", "Poland", "", true)).id();
        String uploaded = mvc.perform(multipart("/cats/"+cat+"/documents").file(new MockMultipartFile("file","own.pdf","application/pdf",pdf)).param("kind","PEDIGREE").with(user("moderator")).with(csrf())).andReturn().getResponse().getContentAsString();
        long id = json.readTree(uploaded).get("id").asLong();
        String requested = mvc.perform(post("/documents/"+id+"/verification-request").with(user("moderator")).with(csrf())).andReturn().getResponse().getContentAsString();
        mvc.perform(get("/moderation/documents").with(user("moderator"))).andExpect(jsonPath("$.total").value(0));
        String decision = json.writeValueAsString(java.util.Map.of("status","VERIFIED","requestedAt",json.readTree(requested).get("verificationRequestedAt").asText(),"note","Own review prohibited"));
        mvc.perform(post("/moderation/documents/"+id+"/decision").with(user("moderator")).with(csrf()).contentType("application/json").content(decision)).andExpect(status().isForbidden());
    }

    @Test void rejectedDocumentCanBeAppealedWithOwnerExplanation() throws Exception {
        int cat = fixture(); long id = upload(cat);
        db.update("INSERT INTO mc_accounts(id,username,password_hash,email,first_name,surname) VALUES (2000000000,'moderator',?,'moderator@example.test','Test','Moderator')", encoder.encode("StrongTestPassword!"));
        String requested = mvc.perform(post("/documents/"+id+"/verification-request").with(user("alice")).with(csrf())).andReturn().getResponse().getContentAsString();
        String rejected = json.writeValueAsString(java.util.Map.of("status","REJECTED","requestedAt",json.readTree(requested).get("verificationRequestedAt").asText(),"note","The scan is not readable enough."));
        mvc.perform(post("/moderation/documents/"+id+"/decision").with(user("moderator")).with(csrf()).contentType("application/json").content(rejected)).andExpect(status().isOk());
        String paddedShortNote = json.writeValueAsString(java.util.Map.of("note", "     short     "));
        mvc.perform(post("/documents/"+id+"/verification-appeal").with(user("alice")).with(csrf())
            .contentType("application/json").content(paddedShortNote)).andExpect(status().isBadRequest());
        mvc.perform(post("/documents/"+id+"/verification-appeal").with(user("alice")).with(csrf()).contentType("application/json").content("{\"note\":\"Here is the issuing registry reference.\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.verification").value("REVIEW_REQUESTED"))
            .andExpect(jsonPath("$.verificationAppealNote").value("Here is the issuing registry reference."));
        mvc.perform(post("/documents/"+id+"/verification-appeal").with(user("bob")).with(csrf()).contentType("application/json").content("{\"note\":\"Not my file appeal.\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/documents/"+id+"/verification-appeal").with(user("alice")).with(csrf()).contentType("application/json").content("{\"note\":\"short\"}"))
            .andExpect(status().isBadRequest());
    }
}
