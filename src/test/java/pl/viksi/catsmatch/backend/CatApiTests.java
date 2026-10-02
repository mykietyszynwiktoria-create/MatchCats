package pl.viksi.catsmatch.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import pl.viksi.catsmatch.backend.account.*;
import pl.viksi.catsmatch.backend.cats.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class CatApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AccountService accounts;
    @Autowired CatRepository cats;
    @Autowired BreederRepository breeders;
    @Autowired CatPhotoRepository photos;
    @Test void photosAreDecodedReencodedAndOnlyOwnersCanChangeThem() throws Exception {
        account("photoalice");account("photobob");breeder("photoalice");breeder("photobob");
        int id=create("photoalice",profile("Photo Cat","Maine Coon","FEMALE"));
        var image=new java.awt.image.BufferedImage(3,2,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"PNG",bytes);
        var file=new MockMultipartFile("file","cat.png","image/png",bytes.toByteArray());
        mvc.perform(multipart("/cats/"+id+"/photo").file(file).with(user("photobob")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(multipart("/cats/"+id+"/photo").file(file).with(user("photoalice"))).andExpect(status().isForbidden());
        mvc.perform(multipart("/cats/"+id+"/photo").file(file).with(user("photoalice")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/cats/"+id).with(user("photoalice"))).andExpect(jsonPath("$.hasPhoto").value(true));
        mvc.perform(get("/cats/"+id+"/photo")).andExpect(status().isUnauthorized());
        byte[] saved=mvc.perform(get("/cats/"+id+"/photo").with(user("photobob"))).andExpect(status().isOk())
            .andExpect(content().contentType("image/jpeg")).andReturn().getResponse().getContentAsByteArray();
        assertEquals(3,javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(saved)).getWidth());
        mvc.perform(delete("/cats/"+id+"/photo").with(user("photobob")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(delete("/cats/"+id).with(user("photoalice")).with(csrf())).andExpect(status().isNoContent());
        assertFalse(photos.existsById(id));
    }
    @Test void invalidPhotoBytesAreRejectedEvenWithAnImageFilename() throws Exception {
        account("badphoto");breeder("badphoto");int id=create("badphoto",profile("Photo Cat","Maine Coon","FEMALE"));
        var fake=new MockMultipartFile("file","cat.jpg","image/jpeg","<svg onload='alert(1)'/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mvc.perform(multipart("/cats/"+id+"/photo").file(fake).with(user("badphoto")).with(csrf())).andExpect(status().isBadRequest());
        assertFalse(photos.existsById(id));
    }
    int account(String name) {
        return accounts.register(new AccountService.Registration(name,"StrongTestPassword!",name+"@example.test","Test","Breeder")).id();
    }
    void breeder(String username) throws Exception {
        mvc.perform(put("/owners/me").with(user(username)).with(csrf()).contentType("application/json")
            .content(json.writeValueAsString(Map.of("kennel","Blue Cats","city","Warsaw","country","Poland","bio",""))))
            .andExpect(status().isOk());
    }
    Map<String,Object> profile(String name, String breed, String sex) {
        return new HashMap<>(Map.of("name",name,"breed",breed,"sex",sex,"health","HEALTHY",
            "birthDate","2022-01-01","city","Warsaw","country","Poland","description","Owner supplied description","available",true));
    }
    int create(String username, Map<String,Object> data) throws Exception {
        var response=mvc.perform(post("/cats").with(user(username)).with(csrf()).contentType("application/json").content(json.writeValueAsString(data)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asInt();
    }
    @Test void breederProfileUsesAuthenticatedIdentityAndExcludesAccountSecrets() throws Exception {
        int id=account("alice");account("bob");
        mvc.perform(get("/owners/me").with(user("alice"))).andExpect(status().isNotFound());
        breeder("alice");breeder("bob");
        mvc.perform(get("/owners/"+id).with(user("bob")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.kennel").value("Blue Cats"))
            .andExpect(jsonPath("$.email").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(put("/owners/me").with(user("alice")).with(csrf()).contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
    }
    @Test void catsPersistAndOnlyTheirOwnerCanEditOrDelete() throws Exception {
        int alice=account("alice"), bob=account("bob");breeder("alice");breeder("bob");
        var data=profile("Luna","Maine Coon","FEMALE");data.put("ownerId",bob);
        int id=create("alice",data);
        assertEquals(alice,cats.findById(id).orElseThrow().ownerId);
        mvc.perform(get("/cats/"+id).with(user("bob"))).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Luna"));
        data.put("name","Luna Updated");var update=Map.of("version",0,"profile",data);
        mvc.perform(put("/cats/"+id).with(user("bob")).with(csrf()).contentType("application/json").content(json.writeValueAsString(update)))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/cats/"+id).with(user("bob")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(put("/cats/"+id).with(user("alice")).with(csrf()).contentType("application/json").content(json.writeValueAsString(update)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Luna Updated")).andExpect(jsonPath("$.version").value(1));
        mvc.perform(put("/cats/"+id).with(user("alice")).with(csrf()).contentType("application/json").content(json.writeValueAsString(update)))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("STALE_VERSION"));
        mvc.perform(delete("/cats/"+id).with(user("alice")).with(csrf())).andExpect(status().isNoContent());
        assertFalse(cats.existsById(id));
        mvc.perform(get("/cats/"+id).with(user("alice"))).andExpect(status().isNotFound());
    }
    @Test void searchFiltersAndPaginationReturnDeterministicResults() throws Exception {
        account("alice");account("bob");breeder("alice");breeder("bob");
        int luna=create("alice",profile("Luna","Maine Coon","FEMALE"));
        create("bob",profile("Oliver","Maine Coon","MALE"));
        var unavailable=profile("Bella","Ragdoll","FEMALE");unavailable.put("available",false);create("bob",unavailable);
        mvc.perform(get("/cats").with(user("alice")).param("breed"," maine coon ").param("sex","FEMALE").param("city","warsaw").param("available","true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].id").value(luna));
        mvc.perform(get("/cats").with(user("alice")).param("page","1").param("size","1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(3)).andExpect(jsonPath("$.items[0].name").value("Oliver"));
        mvc.perform(get("/owners/me/cats").with(user("alice"))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/owners/me/cats").with(user("bob"))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2));
        mvc.perform(get("/cats").with(user("alice")).param("breed","x' OR 1=1 --"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }
    @Test void invalidDataMissingProfileAndAccessAreRejected() throws Exception {
        account("alice");
        mvc.perform(post("/cats").with(user("alice")).with(csrf()).contentType("application/json").content(json.writeValueAsString(profile("Luna","Maine Coon","FEMALE"))))
            .andExpect(status().isNotFound());
        breeder("alice");
        var data=profile("Luna","Maine Coon","FEMALE");data.put("birthDate",LocalDate.now().plusDays(1).toString());
        mvc.perform(post("/cats").with(user("alice")).with(csrf()).contentType("application/json").content(json.writeValueAsString(data)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.birthDate").exists());
        data.put("birthDate","2022-01-01");data.put("health","SICK");
        mvc.perform(post("/cats").with(user("alice")).with(csrf()).contentType("application/json").content(json.writeValueAsString(data)))
            .andExpect(status().isBadRequest());
        data.put("available",false);int id=create("alice",data);
        mvc.perform(get("/cats/"+id)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/cats/"+id).with(user("alice"))).andExpect(status().isForbidden());
        mvc.perform(get("/cats").with(user("alice")).param("size","101")).andExpect(status().isBadRequest());
        mvc.perform(get("/cats").with(user("alice")).param("page","-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/cats").with(user("alice")).param("sex","INVALID")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(put("/cats/"+id).with(user("alice")).with(csrf()).contentType("application/json").content("{\"version\":0,\"profile\":{}}"))
            .andExpect(status().isBadRequest());
    }
}
