package pl.viksi.catsmatch.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.*;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.*;
import pl.viksi.catsmatch.backend.account.AccountRepository;
import java.util.*;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean UserDetailsService userDetailsService(AccountRepository accounts) {
        return username -> accounts.findByUsername(username.toLowerCase(Locale.ROOT))
            .map(a -> User.withUsername(a.username).password(a.passwordHash).disabled(a.suspended).roles("BREEDER").build())
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }
    @Bean AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(users); provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
    @Bean SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository());
    }
    @Bean CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.origins:http://127.0.0.1:8765,http://localhost:8765}") String origins) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.asList(origins.split(",")));
        cors.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type","X-CSRF-TOKEN")); cors.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/**", cors);return source;
    }
    @Bean SecurityFilterChain security(HttpSecurity http, SecurityContextRepository repository, AccountRepository accounts) throws Exception {
        http.addFilterAfter(new SessionVersionFilter(accounts), org.springframework.security.web.context.SecurityContextHolderFilter.class);
        http.cors(c -> {}).securityContext(c -> c.securityContextRepository(repository))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.GET, "/health", "/health/ready", "/auth/csrf", "/billing/plans", "/", "/index.html",
                    "/styles.css", "/sky-garden.css", "/live.css", "/i18n.js", "/api.js",
                    "/live.js", "/safety.js", "/error-pages.js", "/legal.js", "/plans.js", "/app.js", "/manifest.webmanifest", "/service-worker.js", "/assets/*.jpg").permitAll()
                .requestMatchers(HttpMethod.POST, "/users", "/auth/login", "/auth/password/request", "/auth/password/reset", "/auth/email/confirm").permitAll()
                .requestMatchers("/error").permitAll().anyRequest().authenticated())
            .requestCache(c -> c.disable()).formLogin(c -> c.disable()).httpBasic(c -> c.disable())
            .headers(h -> h
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data:; connect-src 'self'; object-src 'none'; " +
                    "base-uri 'self'; frame-ancestors 'none'; form-action 'self'"))
                .referrerPolicy(r -> r.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "camera=(), microphone=(), geolocation=()")))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req,res,ex) -> {
                    res.setStatus(401);res.setContentType("application/json");
                    res.getWriter().write("{\"status\":401,\"code\":\"UNAUTHENTICATED\",\"message\":\"Sign in first\",\"fields\":{}}");
                })
                .accessDeniedHandler((req,res,ex) -> {
                    res.setStatus(403);res.setContentType("application/json");
                    res.getWriter().write("{\"status\":403,\"code\":\"FORBIDDEN\",\"message\":\"Access denied or invalid CSRF token\",\"fields\":{}}");
                }))
            .logout(l -> l.logoutUrl("/auth/logout").logoutSuccessHandler((req,res,a) -> res.setStatus(204)));
        return http.build();
    }
}

