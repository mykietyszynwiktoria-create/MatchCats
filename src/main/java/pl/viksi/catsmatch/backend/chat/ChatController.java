package pl.viksi.catsmatch.backend.chat;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.viksi.catsmatch.backend.cats.CatService;

@RestController
public class ChatController {
    private final ChatService chats;
    public ChatController(ChatService chats) { this.chats = chats; }

    @PostMapping("/cats/{catId}/contact")
    public ChatService.ChatView contact(@PathVariable int catId, Authentication auth) {
        return chats.contact(catId, auth);
    }

    @GetMapping("/chats")
    public CatService.PageView<ChatService.ChatView> list(Authentication auth,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return chats.list(auth, page, size);
    }

    @GetMapping("/chats/{id}")
    public ChatService.ChatView get(@PathVariable long id, Authentication auth) {
        return chats.get(id, auth);
    }

    @GetMapping("/chats/{id}/messages")
    public CatService.PageView<ChatService.MessageView> history(@PathVariable long id, Authentication auth,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return chats.history(id, auth, page, size);
    }

    @PostMapping("/chats/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatService.MessageView send(@PathVariable long id, Authentication auth,
                                       @Valid @RequestBody ChatService.MessageInput input) {
        return chats.send(id, auth, input);
    }
}
