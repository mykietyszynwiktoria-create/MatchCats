package pl.viksi.catsmatch.backend.chat;

import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.viksi.catsmatch.backend.account.AccountRepository;
import pl.viksi.catsmatch.backend.cats.*;
import pl.viksi.catsmatch.backend.common.ApiException;
import java.time.Instant;
import java.util.List;

@Service
public class ChatService {
    public record MessageInput(@NotBlank @Size(max = 4000) String text) {}
    public record MessageView(Long id, Long conversationId, Integer authorId, String text, Instant createdAt) {}
    public record ChatView(Long id, Integer firstOwnerId, Integer secondOwnerId, Integer contextCatId, Instant createdAt, boolean contactBlocked) {}

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final AccountRepository accounts;
    private final CatService cats;
    private final pl.viksi.catsmatch.backend.safety.SafetyService safety;

    public ChatService(ConversationRepository conversations, MessageRepository messages,
                       AccountRepository accounts, CatService cats, pl.viksi.catsmatch.backend.safety.SafetyService safety) {
        this.conversations = conversations;
        this.messages = messages;
        this.accounts = accounts;
        this.cats = cats;
        this.safety = safety;
    }

    private ChatView view(Conversation c) {
        boolean unavailable=accounts.findById(c.firstOwnerId).map(a->a.suspended).orElse(true)
            || accounts.findById(c.secondOwnerId).map(a->a.suspended).orElse(true);
        return new ChatView(c.id, c.firstOwnerId, c.secondOwnerId, c.contextCatId, c.createdAt, unavailable || safety.blocked(c.firstOwnerId,c.secondOwnerId));
    }

    private Conversation accessible(long id, int owner) {
        Conversation c = conversations.findById(id).orElseThrow(() -> ApiException.missing("Conversation"));
        if (!c.firstOwnerId.equals(owner) && !c.secondOwnerId.equals(owner)) {
            throw ApiException.forbidden();
        }
        return c;
    }

    private Pageable page(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > 100) {
            throw ApiException.invalid("Page must be non-negative and size between 1 and 100");
        }
        return PageRequest.of(page, size, sort);
    }

    @Transactional
    public ChatView contact(int catId, Authentication auth) {
        int owner = cats.userId(auth);
        cats.breeder(owner);
        Cat cat = cats.cat(catId);
        if (cat.ownerId.equals(owner)) throw ApiException.invalid("You cannot contact yourself");
        if (!cat.available || cat.health != Cat.Health.HEALTHY) {
            throw ApiException.invalid("The cat is not available for contact");
        }
        int first = Math.min(owner, cat.ownerId);
        int second = Math.max(owner, cat.ownerId);
        // Both directions acquire the same locks in the same order.
        // A second request waits, then reuses the conversation created by the first.
        accounts.lockAccounts(List.of(first, second));
        safety.requireContact(first, second);
        Conversation conversation = conversations.findByFirstOwnerIdAndSecondOwnerId(first, second)
            .orElseGet(() -> conversations.saveAndFlush(new Conversation(first, second, catId)));
        return view(conversation);
    }

    @Transactional(readOnly = true)
    public CatService.PageView<ChatView> list(Authentication auth, int page, int size) {
        var result = conversations.belongingTo(cats.userId(auth), page(page, size, Sort.by("id").descending()));
        return new CatService.PageView<>(result.map(this::view).getContent(), result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public ChatView get(long id, Authentication auth) {
        return view(accessible(id, cats.userId(auth)));
    }

    @Transactional(readOnly = true)
    public CatService.PageView<MessageView> history(long id, Authentication auth, int page, int size) {
        accessible(id, cats.userId(auth));
        var result = messages.findByConversationId(id, page(page, size, Sort.by("id")));
        return new CatService.PageView<>(result.map(this::view).getContent(), result.getTotalElements(), page, size);
    }

    @Transactional
    public MessageView send(long id, Authentication auth, MessageInput input) {
        int author = cats.userId(auth);
        Conversation chat=accessible(id, author);
        accounts.lockAccounts(List.of(chat.firstOwnerId,chat.secondOwnerId));
        safety.requireContact(chat.firstOwnerId,chat.secondOwnerId);
        return view(messages.saveAndFlush(new Message(id, author, input.text().strip())));
    }

    private MessageView view(Message message) {
        return new MessageView(message.id, message.conversationId, message.authorId, message.text, message.createdAt);
    }
}
