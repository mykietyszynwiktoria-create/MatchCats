package pl.viksi.catsmatch.backend.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.viksi.catsmatch.backend.account.AccountRepository;
import java.io.IOException;

public class SessionVersionFilter extends OncePerRequestFilter {
    public static final String ATTRIBUTE="matchcats.securityVersion";
    private final AccountRepository accounts;
    public SessionVersionFilter(AccountRepository accounts) { this.accounts=accounts; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        var session=request.getSession(false);
        if(auth!=null && auth.isAuthenticated() && session!=null && session.getAttribute(ATTRIBUTE) instanceof Long version) {
            var account=accounts.findByUsername(auth.getName());
            if(account.isEmpty() || account.get().suspended || account.get().securityVersion!=version) {
                session.invalidate();SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request,response);
    }
}
