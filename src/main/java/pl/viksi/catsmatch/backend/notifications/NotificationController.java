package pl.viksi.catsmatch.backend.notifications;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service=service; }
    @GetMapping("/notifications")
    List<NotificationService.NotificationView> list(Authentication auth, @RequestParam(defaultValue="50") int limit) { return service.list(auth,limit); }
    @GetMapping("/notifications/unread-count")
    NotificationService.UnreadCount unreadCount(Authentication auth) { return service.unreadCount(auth); }
    @PostMapping("/notifications/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void read(@PathVariable long id, Authentication auth) { service.read(id,auth); }
}

