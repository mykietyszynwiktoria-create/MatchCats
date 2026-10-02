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

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class DocumentApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AccountService accounts;
    @Autowired CatService cats;
    @Autowired DocumentRepository documents;
    @Autowired EntityManager entities;
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
}
