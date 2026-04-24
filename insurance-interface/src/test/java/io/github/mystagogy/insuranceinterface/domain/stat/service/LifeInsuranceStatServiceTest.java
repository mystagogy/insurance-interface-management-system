package io.github.mystagogy.insuranceinterface.domain.stat.service;

import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiCallHistory;
import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiInfo;
import io.github.mystagogy.insuranceinterface.domain.api.repository.ApiInfoRepository;
import io.github.mystagogy.insuranceinterface.domain.api.service.ApiCallHistoryAuditService;
import io.github.mystagogy.insuranceinterface.domain.auth.repository.UserRepository;
import io.github.mystagogy.insuranceinterface.domain.stat.client.LifeInsuranceApiClient;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.LifeInsuranceStatQueryRequest;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.LifeInsuranceStatResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.external.LifeInsuranceExternalItem;
import io.github.mystagogy.insuranceinterface.domain.stat.entity.GenderType;
import io.github.mystagogy.insuranceinterface.domain.stat.entity.LifeInsuranceStat;
import io.github.mystagogy.insuranceinterface.domain.stat.repository.LifeInsuranceStatRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LifeInsuranceStatServiceTest {

    @Mock
    private LifeInsuranceStatRepository lifeInsuranceStatRepository;

    @Mock
    private ApiInfoRepository apiInfoRepository;

    @Mock
    private ApiCallHistoryAuditService apiCallHistoryAuditService;

    @Mock
    private LifeInsuranceApiClient lifeInsuranceApiClient;

    @Mock
    private StatFailureResolver failureResolver;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LifeInsuranceStatService lifeInsuranceStatService;

    @Test
    void getStoredSubscriptionStatsReturnsSavedDataWithoutExternalCall() {
        ApiInfo apiInfo = new ApiInfo(LifeInsuranceStatService.API_NAME, "금융위원회", "http://example.com", 5000);
        LifeInsuranceStat existing = new LifeInsuranceStat(
            apiInfo,
            LocalDate.of(2023, 1, 1),
            "전국",
            "40대",
            GenderType.FEMALE,
            "종신보험",
            100L,
            new BigDecimal("0.1234"),
            "{}"
        );

        when(apiInfoRepository.findByApiNameAndUseYnTrue(LifeInsuranceStatService.API_NAME)).thenReturn(Optional.of(apiInfo));
        when(lifeInsuranceStatRepository.findStats(
            apiInfo,
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2023, 12, 31)
        )).thenReturn(List.of(existing));

        LifeInsuranceStatResponse response = lifeInsuranceStatService.getStoredSubscriptionStats(
            new LifeInsuranceStatQueryRequest("2023", "2023")
        );

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.items().get(0).insuranceType()).isEqualTo("종신보험");
        assertThat(response.items().get(0).areaName()).isEqualTo("전국");
        verify(lifeInsuranceApiClient, never()).fetch(any(), any());
        verify(lifeInsuranceStatRepository, never()).upsertAll(any());
        verify(apiCallHistoryAuditService, never()).createPending(any(), any(), any());
    }

    @Test
    void refreshSubscriptionStatsFetchesExternalDataAndCompletesHistory() {
        ApiInfo apiInfo = new ApiInfo(LifeInsuranceStatService.API_NAME, "금융위원회", "http://example.com", 5000);
        ApiCallHistory pendingHistory = new ApiCallHistory(apiInfo, null, "fromYear=2023,toYear=2023");
        ReflectionTestUtils.setField(pendingHistory, "id", 10L);
        LifeInsuranceStat existing = new LifeInsuranceStat(
            apiInfo,
            LocalDate.of(2023, 1, 1),
            "전국",
            "40대",
            GenderType.FEMALE,
            "종신보험",
            100L,
            new BigDecimal("0.1234"),
            "{}"
        );

        when(apiInfoRepository.findByApiNameAndUseYnTrue(LifeInsuranceStatService.API_NAME)).thenReturn(Optional.of(apiInfo));
        when(apiCallHistoryAuditService.createPending(any(), any(), any())).thenReturn(pendingHistory);
        when(lifeInsuranceApiClient.fetch("2023", "2023")).thenReturn(List.of(
            new LifeInsuranceExternalItem(
                LocalDate.of(2023, 1, 1),
                "전국",
                "40대",
                GenderType.FEMALE,
                "종신보험",
                100L,
                new BigDecimal("0.1234"),
                "{}"
            )
        ));
        when(lifeInsuranceStatRepository.findStats(
            apiInfo,
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2023, 12, 31)
        )).thenReturn(List.of(existing));

        LifeInsuranceStatResponse response = lifeInsuranceStatService.refreshSubscriptionStats(
            new LifeInsuranceStatQueryRequest("2023", "2023")
        );

        assertThat(response.totalCount()).isEqualTo(1);
        verify(lifeInsuranceApiClient).fetch("2023", "2023");
        verify(lifeInsuranceStatRepository).upsertAll(any());
        verify(apiCallHistoryAuditService).recordSuccess(10L, 200, "fetched life insurance subscription statistics");
    }

    @Test
    void externalFailureWritesErrorLogAndMarksHistoryFail() {
        ApiInfo apiInfo = new ApiInfo(LifeInsuranceStatService.API_NAME, "금융위원회", "http://example.com", 5000);
        ApiCallHistory pendingHistory = new ApiCallHistory(apiInfo, null, "fromYear=2023,toYear=2023");
        ReflectionTestUtils.setField(pendingHistory, "id", 2L);

        when(apiInfoRepository.findByApiNameAndUseYnTrue(LifeInsuranceStatService.API_NAME)).thenReturn(Optional.of(apiInfo));
        when(apiCallHistoryAuditService.createPending(any(), any(), any())).thenReturn(pendingHistory);
        when(lifeInsuranceApiClient.fetch("2023", "2023"))
            .thenThrow(new IllegalStateException("생명보험 가입정보 API 응답이 비어 있습니다."));
        when(failureResolver.resolve(any(), any(), any(), any())).thenReturn(
            new StatFailureResolver.ResolvedFailure(
                500,
                "생명보험 가입정보 외부 API 호출에 실패했습니다.",
                "life insurance subscription api failed",
                io.github.mystagogy.insuranceinterface.domain.log.entity.ErrorType.BAD_RESPONSE,
                null
            )
        );

        assertThatThrownBy(() -> lifeInsuranceStatService.getSubscriptionStats(
            new LifeInsuranceStatQueryRequest("2023", "2023")
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("생명보험 가입정보 조회에 실패했습니다.");

        verify(apiCallHistoryAuditService).recordFailure(
            eq(2L),
            eq(apiInfo),
            eq(500),
            eq("생명보험 가입정보 외부 API 호출에 실패했습니다."),
            eq("life insurance subscription api failed"),
            any(),
            any()
        );
    }

    @Test
    void rejectsInvalidPeriodOrder() {
        assertThatThrownBy(() -> lifeInsuranceStatService.getSubscriptionStats(
            new LifeInsuranceStatQueryRequest("2024", "2023")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("조회 시작년도는 종료년도보다 늦을 수 없습니다.");
    }
}
