package io.github.mystagogy.insuranceinterface.domain.auth.controller;

import io.github.mystagogy.insuranceinterface.domain.auth.entity.User;
import io.github.mystagogy.insuranceinterface.domain.auth.entity.UserRole;
import io.github.mystagogy.insuranceinterface.domain.auth.repository.UserRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
        userRepository.deleteAll();
        userRepository.save(
            new User("operator1", passwordEncoder.encode("testpass123!"), "운영자1", UserRole.ROLE_OPERATOR)
        );
    }

    @Test
    void loginSuccessAndCanGetMyInfo() throws Exception {
        MvcResult loginResult = mockMvc.perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "username": "operator1",
                          "password": "testpass123!"
                        }
                        """)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.username").value("운영자1"))
            .andExpect(jsonPath("$.data.role").value("ROLE_OPERATOR"))
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(
                get("/auth/me").session(session)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.username").value("운영자1"))
            .andExpect(jsonPath("$.data.role").value("ROLE_OPERATOR"));
    }

    @Test
    void loginFailReturnsUnauthorized() throws Exception {
        mockMvc.perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "username": "operator1",
                          "password": "wrong-password"
                        }
                        """)
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    void protectedEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/dashboard/summary").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("로그인이 필요합니다."));
    }

    @Test
    void rootPathRedirectsToLoginPage() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void unauthenticatedHtmlPageRequestRedirectsToLoginPage() throws Exception {
        mockMvc.perform(get("/dashboard").accept(MediaType.TEXT_HTML))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?required=true"));
    }

    @Test
    void unauthenticatedHtmlApiRequestRedirectsToLoginPage() throws Exception {
        mockMvc.perform(get("/dashboard/summary").accept(MediaType.TEXT_HTML))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?required=true"));
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/login.html"));
    }

    @Test
    void dashboardPageIsAccessibleAfterLogin() throws Exception {
        MvcResult loginResult = login("operator1", "testpass123!");
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/dashboard").session(session))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/dashboard.html"));
    }

    @Test
    void historyPageIsAccessibleAfterLogin() throws Exception {
        MvcResult loginResult = login("operator1", "testpass123!");
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/history").session(session))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/history.html"));
    }

    @Test
    void logoutSuccessReturnsApiResponse() throws Exception {
        MvcResult loginResult = login("operator1", "testpass123!");
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(
                post("/auth/logout")
                    .session(session)
                    .with(csrf())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
            .andExpect(jsonPath("$.error").value(Matchers.nullValue()));
    }

    @Test
    void logoutWithoutCsrfReturnsForbidden() throws Exception {
        MvcResult loginResult = login("operator1", "testpass123!");
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(
                post("/auth/logout")
                    .session(session)
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("접근 권한이 없습니다."));
    }

    private MvcResult login(String username, String password) throws Exception {
        return mockMvc.perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, password))
            )
            .andReturn();
    }
}
