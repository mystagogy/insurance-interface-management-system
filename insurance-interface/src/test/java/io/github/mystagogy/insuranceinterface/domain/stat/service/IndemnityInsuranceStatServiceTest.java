package io.github.mystagogy.insuranceinterface.domain.stat.service;

import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiCallHistory;
import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiInfo;
import io.github.mystagogy.insuranceinterface.domain.api.repository.ApiInfoRepository;
import io.github.mystagogy.insuranceinterface.domain.api.service.ApiCallHistoryAuditService;
import io.github.mystagogy.insuranceinterface.domain.auth.repository.UserRepository;
import io.github.mystagogy.insuranceinterface.domain.stat.client.IndemnityInsuranceApiClient;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.IndemnityInsuranceStatQueryRequest;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.IndemnityInsuranceStatResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.external.IndemnityInsuranceExternalItem;
import io.github.mystagogy.insuranceinterface.domain.stat.entity.GenderType;
import io.github.mystagogy.insuranceinterface.domain.stat.entity.IndemnityInsuranceStat;
import io.github.mystagogy.insuranceinterface.domain.stat.repository.IndemnityInsuranceStatRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndemnityInsuranceStatServiceTest {

    @Mock
    private IndemnityInsuranceStatRepository indemnityInsuranceStatRepository;

    @Mock
    private ApiInfoRepository apiInfoRepository;

    @Mock
    private ApiCallHistoryAuditService apiCallHistoryAuditService;

    @Mock
    private IndemnityInsuranceApiClient indemnityInsuranceApiClient;

    @Mock
    private StatFailureResolver failureResolver;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private IndemnityInsuranceStatService indemnityInsuranceStatService;

    @Test
    void requestAlwaysFetchesExternalDataEvenWhenStatsAlreadyExist() {
        ApiInfo apiInfo = new ApiInfo(IndemnityInsuranceStatService.API_NAME, "금융위원회", "http://example.com", 5000);
        ApiCallHistory pendingHistory = new ApiCallHistory(apiInfo, null, "fromYm=202401,toYm=202401");
        ReflectionTestUtils.setField(pendingHistory, "id", 10L);
        IndemnityInsuranceStat existing = new IndemnityInsuranceStat(
            apiInfo,
            LocalDate.of(2024, 1, 1),
            "40",
            GenderType.MALE,
            "4세대 실손의료보험",
            "질병",
            new BigDecimal("12345"),
            "{}"
        );

        when(apiInfoRepository.findByApiNameAndUseYnTrue(IndemnityInsuranceStatService.API_NAME)).thenReturn(Optional.of(apiInfo));
        when(apiCallHistoryAuditService.createPending(any(), any(), any())).thenReturn(pendingHistory);
        when(indemnityInsuranceApiClient.fetch("202401", "202401")).thenReturn(List.of(
            new IndemnityInsuranceExternalItem(
                LocalDate.of(2024, 1, 1),
                "40",
                GenderType.MALE,
                "4세대 실손의료보험",
                "질병",
                new BigDecimal("12345"),
                "{}"
            )
        ));
        when(indemnityInsuranceStatRepository.findStats(
            apiInfo,
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 31)
        )).thenReturn(List.of(existing));

        IndemnityInsuranceStatResponse response = indemnityInsuranceStatService.getSubscriptionStats(
            new IndemnityInsuranceStatQueryRequest("202401", "202401")
        );

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.items().get(0).indemnityType()).isEqualTo("4세대 실손의료보험");
        assertThat(response.items().get(0).coverageItem()).isEqualTo("질병");
        verify(indemnityInsuranceApiClient).fetch("202401", "202401");
        verify(indemnityInsuranceStatRepository).upsertAll(any());
        verify(apiCallHistoryAuditService).recordSuccess(10L, 200, "fetched indemnity insurance subscription statistics");
    }

    @Test
    void externalFailureWritesErrorLogAndMarksHistoryFail() {
        ApiInfo apiInfo = new ApiInfo(IndemnityInsuranceStatService.API_NAME, "금융위원회", "http://example.com", 5000);
        ApiCallHistory pendingHistory = new ApiCallHistory(apiInfo, null, "fromYm=202401,toYm=202401");
        ReflectionTestUtils.setField(pendingHistory, "id", 2L);

        when(apiInfoRepository.findByApiNameAndUseYnTrue(IndemnityInsuranceStatService.API_NAME)).thenReturn(Optional.of(apiInfo));
        when(apiCallHistoryAuditService.createPending(any(), any(), any())).thenReturn(pendingHistory);
        when(indemnityInsuranceApiClient.fetch("202401", "202401"))
            .thenThrow(new IllegalStateException("실손보험 가입정보 API 응답이 비어 있습니다."));
        when(failureResolver.resolve(any(), any(), any(), any())).thenReturn(
            new StatFailureResolver.ResolvedFailure(
                500,
                "실손보험 가입정보 외부 API 호출에 실패했습니다.",
                "indemnity insurance subscription api failed",
                io.github.mystagogy.insuranceinterface.domain.log.entity.ErrorType.BAD_RESPONSE,
                null
            )
        );

        assertThatThrownBy(() -> indemnityInsuranceStatService.getSubscriptionStats(
            new IndemnityInsuranceStatQueryRequest("202401", "202401")
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("실손보험 가입정보 조회에 실패했습니다.");

        verify(apiCallHistoryAuditService).recordFailure(
            eq(2L),
            eq(apiInfo),
            eq(500),
            eq("실손보험 가입정보 외부 API 호출에 실패했습니다."),
            eq("indemnity insurance subscription api failed"),
            any(),
            any()
        );
    }

    @Test
    void rejectsInvalidPeriodOrder() {
        assertThatThrownBy(() -> indemnityInsuranceStatService.getSubscriptionStats(
            new IndemnityInsuranceStatQueryRequest("202402", "202401")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("조회 시작년월은 종료년월보다 늦을 수 없습니다.");
    }
}
