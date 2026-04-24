package io.github.mystagogy.insuranceinterface.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationProvider authenticationProvider(
        UserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/auth/login")
            )
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/auth/login", "/login", "/login.html", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error")
                .permitAll()
                .anyRequest()
                .authenticated()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> writeSuccessResponse(response, HttpStatus.OK))
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> handleAuthenticationEntryPoint(request, response))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                    writeErrorResponse(request, response, HttpStatus.FORBIDDEN, "접근 권한이 없습니다.")
                )
            )
            .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);
        return http.build();
    }

    private void handleAuthenticationEntryPoint(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (isHtmlNavigationRequest(request)) {
            response.sendRedirect("/login?required=true");
            return;
        }
        writeErrorResponse(request, response, HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
    }

    private boolean isHtmlNavigationRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.TEXT_HTML_VALUE);
    }

    private void writeSuccessResponse(HttpServletResponse response, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"success\":true,\"data\":null,\"error\":null}");
    }

    private void writeErrorResponse(
        HttpServletRequest request,
        HttpServletResponse response,
        HttpStatus status,
        String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = String.format(
            "{\"success\":false,\"data\":null,\"error\":{\"timestamp\":\"%s\",\"path\":\"%s\",\"message\":\"%s\"}}",
            Instant.now(),
            escapeJson(request.getRequestURI()),
            escapeJson(message)
        );
        response.getWriter().write(body);
    }

    private static final class CsrfCookieFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
        ) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }

    private String escapeJson(String source) {
        return source.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
