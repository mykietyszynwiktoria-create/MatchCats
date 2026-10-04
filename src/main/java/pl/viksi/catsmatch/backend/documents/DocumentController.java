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
        return file(document);
    }

    @GetMapping("/moderation/documents")
    public pl.viksi.catsmatch.backend.cats.CatService.PageView<DocumentView> queue(Authentication auth,
        @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return documents.queue(auth, page, size);
    }

    @GetMapping("/moderation/documents/{id}/download")
    public ResponseEntity<byte[]> reviewDownload(@PathVariable long id, Authentication auth) {
        return file(documents.reviewDownload(id, auth));
    }

    @PostMapping("/moderation/documents/{id}/decision")
    public DocumentView decision(@PathVariable long id, Authentication auth, @Valid @RequestBody DocumentService.Decision input) {
        return documents.decide(id, auth, input);
    }

    private ResponseEntity<byte[]> file(CatDocument document) {
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

    @PostMapping("/documents/{id}/verification-request")
    public DocumentView requestVerification(@PathVariable long id, Authentication auth) {
        return documents.requestVerification(id, auth);
    }

    @DeleteMapping("/documents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, Authentication auth) {
        documents.delete(id, auth);
    }
}
