package io.github.mystagogy.insuranceinterface.domain.stat.controller;

import io.github.mystagogy.insuranceinterface.domain.auth.entity.User;
import io.github.mystagogy.insuranceinterface.domain.auth.entity.UserRole;
import io.github.mystagogy.insuranceinterface.domain.auth.repository.UserRepository;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.LifeInsuranceStatItemResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.LifeInsuranceStatQueryRequest;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.LifeInsuranceStatResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.service.LifeInsuranceStatService;
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
    "spring.datasource.url=jdbc:h2:mem:lifestatdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class LifeInsuranceStatControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private LifeInsuranceStatService lifeInsuranceStatService;

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
    void authenticatedRequestReturnsLifeInsuranceStats() throws Exception {
        MockHttpSession session = loginSession();
        LifeInsuranceStatResponse response = new LifeInsuranceStatResponse(
            "2023",
            "2023",
            1,
            List.of(new LifeInsuranceStatItemResponse(
                LocalDate.of(2023, 1, 1),
                "전국",
                "40대",
                "여자",
                "종신보험",
                100L,
                new BigDecimal("0.1234")
            ))
        );
        when(lifeInsuranceStatService.getSubscriptionStats(eq(new LifeInsuranceStatQueryRequest("2023", "2023"))))
            .thenReturn(response);

        mockMvc.perform(
                get("/api/v1/stats/life-insurance/subscriptions")
                    .param("fromYear", "2023")
                    .param("toYear", "2023")
                    .session(session)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.fromYear").value("2023"))
            .andExpect(jsonPath("$.data.toYear").value("2023"))
            .andExpect(jsonPath("$.data.totalCount").value(1))
            .andExpect(jsonPath("$.data.items[0].statDate").value("2023-01-01"))
            .andExpect(jsonPath("$.data.items[0].areaName").value("전국"))
            .andExpect(jsonPath("$.data.items[0].insuranceType").value("종신보험"))
            .andExpect(jsonPath("$.data.items[0].subscriptionCount").value(100))
            .andExpect(jsonPath("$.data.items[0].subscriptionRate").value(0.1234));

        verify(lifeInsuranceStatService).getSubscriptionStats(new LifeInsuranceStatQueryRequest("2023", "2023"));
    }

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(
                get("/api/v1/stats/life-insurance/subscriptions")
                    .param("fromYear", "2023")
                    .param("toYear", "2023")
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("로그인이 필요합니다."));

        verifyNoInteractions(lifeInsuranceStatService);
    }

    @Test
    void invalidQueryParameterReturnsBadRequest() throws Exception {
        MockHttpSession session = loginSession();

        mockMvc.perform(
                get("/api/v1/stats/life-insurance/subscriptions")
                    .param("fromYear", "23")
                    .param("toYear", "2023")
                    .session(session)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("조회 시작년도는 yyyy 형식이어야 합니다."));

        verifyNoInteractions(lifeInsuranceStatService);
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
