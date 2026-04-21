package io.github.mystagogy.insuranceinterface.domain.auth.service;

import io.github.mystagogy.insuranceinterface.domain.auth.dto.LoginRequest;
import io.github.mystagogy.insuranceinterface.domain.auth.dto.LoginResponse;
import io.github.mystagogy.insuranceinterface.domain.auth.dto.MyInfoResponse;
import io.github.mystagogy.insuranceinterface.domain.auth.entity.User;
import io.github.mystagogy.insuranceinterface.domain.auth.entity.UserRole;
import io.github.mystagogy.insuranceinterface.domain.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpSession httpSession;

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 로그인 성공 시나리오.
     * 기대 결과: 세션에 보안 컨텍스트가 저장되고, 사용자 정보/권한/세션 ID가 응답된다.
     */
    @Test
    void loginSuccessCreatesSessionAndReturnsUserInfo() {
        LoginRequest request = new LoginRequest("operator1", "password");
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "operator1",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_OPERATOR"))
        );
        User user = new User("operator1", "encoded-password", "운영자1", UserRole.ROLE_OPERATOR);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(httpServletRequest.getSession(true)).thenReturn(httpSession);
        when(httpSession.getId()).thenReturn("session-id-1");
        when(userRepository.findByLoginIdAndUseYnTrue("operator1")).thenReturn(Optional.of(user));

        LoginResponse response = authService.login(request, httpServletRequest);

        assertThat(response.username()).isEqualTo("운영자1");
        assertThat(response.role()).isEqualTo("ROLE_OPERATOR");
        assertThat(response.sessionId()).isEqualTo("session-id-1");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("operator1");
        verify(httpSession).setAttribute(eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY), any());
    }

    /**
     * 로그인 실패 시나리오(잘못된 비밀번호).
     * 기대 결과: 서비스 정책 메시지를 포함한 BadCredentialsException이 발생한다.
     */
    @Test
    void loginFailureThrowsBadCredentialsException() {
        LoginRequest request = new LoginRequest("operator1", "wrong-password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(request, httpServletRequest))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    /**
     * 내 정보 조회 성공 시나리오.
     * 기대 결과: SecurityContext의 인증 사용자 기준으로 사용자명/권한이 반환된다.
     */
    @Test
    void myInfoSuccessReturnsAuthenticatedUserInfo() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "operator1",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_OPERATOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = new User("operator1", "encoded-password", "운영자1", UserRole.ROLE_OPERATOR);
        when(userRepository.findByLoginIdAndUseYnTrue("operator1")).thenReturn(Optional.of(user));

        MyInfoResponse response = authService.myInfo();

        assertThat(response.username()).isEqualTo("운영자1");
        assertThat(response.role()).isEqualTo("ROLE_OPERATOR");
    }

    /**
     * 내 정보 조회 실패 시나리오(미인증 상태).
     * 기대 결과: AuthenticationCredentialsNotFoundException이 발생한다.
     */
    @Test
    void myInfoFailsWhenNotAuthenticated() {
        assertThatThrownBy(() -> authService.myInfo())
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
            .hasMessage("로그인이 필요합니다.");
    }
}
