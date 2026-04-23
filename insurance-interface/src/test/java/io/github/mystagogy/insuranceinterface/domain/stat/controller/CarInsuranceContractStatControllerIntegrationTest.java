package io.github.mystagogy.insuranceinterface.domain.stat.controller;

import io.github.mystagogy.insuranceinterface.domain.auth.entity.User;
import io.github.mystagogy.insuranceinterface.domain.auth.entity.UserRole;
import io.github.mystagogy.insuranceinterface.domain.auth.repository.UserRepository;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.CarInsuranceContractStatItemResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.CarInsuranceContractStatQueryRequest;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.CarInsuranceContractStatResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.service.CarInsuranceContractStatService;
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

import java.math.BigDecimal;
import java.util.List;

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
    "spring.datasource.url=jdbc:h2:mem:carstatdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CarInsuranceContractStatControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private CarInsuranceContractStatService carInsuranceContractStatService;

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
    void authenticatedRequestReturnsCarInsuranceContractStats() throws Exception {
        MockHttpSession session = loginSession();
        CarInsuranceContractStatResponse response = new CarInsuranceContractStatResponse(
            "202401",
            "202401",
            1,
            List.of(new CarInsuranceContractStatItemResponse(
                "202401",
                "개인용",
                "대인배상1",
                "남성",
                "20대 이하",
                "외산",
                "중형",
                10L,
                new BigDecimal("10000.00")
            ))
        );
        when(carInsuranceContractStatService.getContractStats(eq(new CarInsuranceContractStatQueryRequest("202401", "202401"))))
            .thenReturn(response);

        mockMvc.perform(
                get("/api/v1/stats/car-insurance/contracts")
                    .param("fromYm", "202401")
                    .param("toYm", "202401")
                    .session(session)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.fromYm").value("202401"))
            .andExpect(jsonPath("$.data.toYm").value("202401"))
            .andExpect(jsonPath("$.data.totalCount").value(1))
            .andExpect(jsonPath("$.data.items[0].insuranceType").value("개인용"))
            .andExpect(jsonPath("$.data.items[0].coverageType").value("대인배상1"))
            .andExpect(jsonPath("$.data.items[0].contractCount").value(10))
            .andExpect(jsonPath("$.data.items[0].earnedPremium").value(10000.00));

        verify(carInsuranceContractStatService).getContractStats(new CarInsuranceContractStatQueryRequest("202401", "202401"));
    }

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(
                get("/api/v1/stats/car-insurance/contracts")
                    .param("fromYm", "202401")
                    .param("toYm", "202401")
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("로그인이 필요합니다."));

        verifyNoInteractions(carInsuranceContractStatService);
    }

    @Test
    void invalidQueryParameterReturnsBadRequest() throws Exception {
        MockHttpSession session = loginSession();

        mockMvc.perform(
                get("/api/v1/stats/car-insurance/contracts")
                    .param("fromYm", "2024")
                    .param("toYm", "202401")
                    .session(session)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("조회 시작년월은 yyyyMM 형식이어야 합니다."));

        verifyNoInteractions(carInsuranceContractStatService);
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
