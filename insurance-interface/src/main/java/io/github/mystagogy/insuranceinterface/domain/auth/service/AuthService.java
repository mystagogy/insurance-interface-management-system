package io.github.mystagogy.insuranceinterface.domain.auth.service;

import io.github.mystagogy.insuranceinterface.domain.auth.dto.LoginRequest;
import io.github.mystagogy.insuranceinterface.domain.auth.dto.LoginResponse;
import io.github.mystagogy.insuranceinterface.domain.auth.dto.MyInfoResponse;
import io.github.mystagogy.insuranceinterface.domain.auth.entity.User;
import io.github.mystagogy.insuranceinterface.domain.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest httpServletRequest) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (AuthenticationException exception) {
            throw new BadCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        HttpSession session = httpServletRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        User user = userRepository.findByLoginIdAndUseYnTrue(authentication.getName())
            .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("사용자 정보를 찾을 수 없습니다."));
        return new LoginResponse(user.getUserName(), user.getRole().name(), session.getId());
    }

    public MyInfoResponse myInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");
        }

        User user = userRepository.findByLoginIdAndUseYnTrue(authentication.getName())
            .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("사용자 정보를 찾을 수 없습니다."));
        return new MyInfoResponse(user.getUserName(), user.getRole().name());
    }
}
