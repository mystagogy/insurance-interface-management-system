package io.github.mystagogy.insuranceinterface.domain.stat.controller;

import io.github.mystagogy.insuranceinterface.domain.auth.entity.User;
import io.github.mystagogy.insuranceinterface.domain.auth.entity.UserRole;
import io.github.mystagogy.insuranceinterface.domain.auth.repository.UserRepository;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.IndemnityInsuranceStatItemResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.IndemnityInsuranceStatQueryRequest;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.IndemnityInsuranceStatResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.service.IndemnityInsuranceStatService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
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

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:indemnitystatdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class IndemnityInsuranceStatControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private IndemnityInsuranceStatService indemnityInsuranceStatService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();

        userRepository.deleteAll();
        userRepository.save(
            new User("operator1", passwordEncoder.encode("testpass123!"), "운영자1", UserRole.ROLE_OPERATOR)
        );
    }

    @Test
    void authenticatedRequestReturnsIndemnityInsuranceStats() throws Exception {
        MockHttpSession session = loginSession();
        IndemnityInsuranceStatResponse response = new IndemnityInsuranceStatResponse(
            "202401",
            "202401",
            1,
            List.of(new IndemnityInsuranceStatItemResponse(
                LocalDate.of(2024, 1, 1),
                "40",
                "남자",
                "4세대 실손의료보험",
                "질병",
                new BigDecimal("12345")
            ))
        );
        when(indemnityInsuranceStatService.getSubscriptionStats(eq(new IndemnityInsuranceStatQueryRequest("202401", "202401"))))
            .thenReturn(response);

        mockMvc.perform(
                get("/api/v1/stats/indemnity-insurance/subscriptions")
                    .param("fromYm", "202401")
                    .param("toYm", "202401")
                    .session(session)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.fromYm").value("202401"))
            .andExpect(jsonPath("$.data.toYm").value("202401"))
            .andExpect(jsonPath("$.data.totalCount").value(1))
            .andExpect(jsonPath("$.data.items[0].statDate").value("2024-01-01"))
            .andExpect(jsonPath("$.data.items[0].ageGroup").value("40"))
            .andExpect(jsonPath("$.data.items[0].gender").value("남자"))
            .andExpect(jsonPath("$.data.items[0].indemnityType").value("4세대 실손의료보험"))
            .andExpect(jsonPath("$.data.items[0].coverageItem").value("질병"))
            .andExpect(jsonPath("$.data.items[0].premiumAmount").value(12345));

        verify(indemnityInsuranceStatService).getSubscriptionStats(new IndemnityInsuranceStatQueryRequest("202401", "202401"));
    }

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(
                get("/api/v1/stats/indemnity-insurance/subscriptions")
                    .param("fromYm", "202401")
                    .param("toYm", "202401")
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("로그인이 필요합니다."));

        verifyNoInteractions(indemnityInsuranceStatService);
    }

    @Test
    void invalidQueryParameterReturnsBadRequest() throws Exception {
        MockHttpSession session = loginSession();

        mockMvc.perform(
                get("/api/v1/stats/indemnity-insurance/subscriptions")
                    .param("fromYm", "2024")
                    .param("toYm", "202401")
                    .session(session)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("조회 시작년월은 yyyyMM 형식이어야 합니다."));

        verifyNoInteractions(indemnityInsuranceStatService);
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
