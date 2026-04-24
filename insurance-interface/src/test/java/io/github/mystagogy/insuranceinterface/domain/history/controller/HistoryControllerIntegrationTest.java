package io.github.mystagogy.insuranceinterface.domain.history.controller;

import io.github.mystagogy.insuranceinterface.domain.auth.entity.User;
import io.github.mystagogy.insuranceinterface.domain.auth.entity.UserRole;
import io.github.mystagogy.insuranceinterface.domain.auth.repository.UserRepository;
import io.github.mystagogy.insuranceinterface.domain.history.dto.HistoryItemResponse;
import io.github.mystagogy.insuranceinterface.domain.history.dto.HistoryQueryRequest;
import io.github.mystagogy.insuranceinterface.domain.history.service.HistoryService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:historydb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class HistoryControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private HistoryService historyService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();

        userRepository.deleteAll();
        userRepository.save(new User("operator1", passwordEncoder.encode("testpass123!"), "운영자1", UserRole.ROLE_OPERATOR));
    }

    @Test
    void authenticatedRequestReturnsFilteredHistory() throws Exception {
        MockHttpSession session = loginSession();
        when(historyService.getRecent(eq(new HistoryQueryRequest("20260401", "20260407"))))
            .thenReturn(List.of(new HistoryItemResponse(
                "REQ-100",
                "2026-04-07 09:10:11",
                "자동차보험 계약 통계 조회",
                "SUCCESS",
                null
            )));

        mockMvc.perform(
                get("/history/recent")
                    .param("from", "20260401")
                    .param("to", "20260407")
                    .session(session)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].requestId").value("REQ-100"))
            .andExpect(jsonPath("$.data[0].requestTime").value("2026-04-07 09:10:11"))
            .andExpect(jsonPath("$.data[0].interfaceName").value("자동차보험 계약 통계 조회"))
            .andExpect(jsonPath("$.data[0].status").value("SUCCESS"))
            .andExpect(jsonPath("$.data[0].errorMessage").isEmpty());

        verify(historyService).getRecent(new HistoryQueryRequest("20260401", "20260407"));
    }

    @Test
    void invalidDateFormatReturnsBadRequest() throws Exception {
        MockHttpSession session = loginSession();

        mockMvc.perform(
                get("/history/recent")
                    .param("from", "202604")
                    .param("to", "20260407")
                    .session(session)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("조회 시작일자는 yyyyMMdd 형식이어야 합니다."));

        verifyNoInteractions(historyService);
    }

    @Test
    void unauthenticatedRequestReturnsUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/history/recent").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("로그인이 필요합니다."));
    }

    private MockHttpSession loginSession() throws Exception {
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
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }
}
