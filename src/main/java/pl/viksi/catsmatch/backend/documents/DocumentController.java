package pl.viksi.catsmatch.backend.documents;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
public class DocumentController {
    private final DocumentService documents;
    public DocumentController(DocumentService documents) { this.documents = documents; }

    @PostMapping(value = "/cats/{catId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentView upload(@PathVariable int catId, Authentication auth,
        @RequestParam CatDocument.Kind kind, @RequestPart MultipartFile file) throws IOException {
        return documents.upload(catId, auth, kind, file);
    }

    @GetMapping("/cats/{catId}/documents")
    public List<DocumentView> list(@PathVariable int catId, Authentication auth) {
        return documents.list(catId, auth);
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable long id, Authentication auth) {
        CatDocument document = documents.download(id, auth);
        String extension = switch (document.mediaType) {
            case "application/pdf" -> ".pdf";
            case "image/png" -> ".png";
            default -> ".jpg";
        };
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(document.mediaType))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename("document-" + document.id + extension).build().toString())
            .header("X-Content-Type-Options", "nosniff")
            .cacheControl(CacheControl.noStore())
            .body(document.content);
    }

    @PutMapping("/documents/{id}/visibility")
    public DocumentView share(@PathVariable long id, Authentication auth,
                             @Valid @RequestBody DocumentService.Sharing input) {
        return documents.share(id, auth, input);
    }

    @DeleteMapping("/documents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, Authentication auth) {
        documents.delete(id, auth);
    }
}
