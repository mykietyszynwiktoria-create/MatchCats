package pl.viksi.catsmatch.backend.chat;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class ChatReadController {
    private final ChatReadService reads;
    public ChatReadController(ChatReadService reads) { this.reads=reads; }
    @GetMapping("/chats/unread")
    public ChatReadService.UnreadView unread(Authentication auth) { return reads.summary(auth); }
    @PostMapping("/chats/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void mark(@PathVariable long id, Authentication auth,@Valid @RequestBody ChatReadService.ReadInput input) { reads.mark(id,auth,input); }
}
