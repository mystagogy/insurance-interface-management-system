package io.github.mystagogy.insuranceinterface.domain.stat.service;

import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiCallHistory;
import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiInfo;
import io.github.mystagogy.insuranceinterface.domain.api.repository.ApiInfoRepository;
import io.github.mystagogy.insuranceinterface.domain.api.service.ApiCallHistoryAuditService;
import io.github.mystagogy.insuranceinterface.domain.auth.repository.UserRepository;
import io.github.mystagogy.insuranceinterface.domain.stat.client.CarInsuranceContractApiClient;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.CarInsuranceContractStatQueryRequest;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.CarInsuranceContractStatResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.external.CarInsuranceContractExternalItem;
import io.github.mystagogy.insuranceinterface.domain.stat.entity.CarInsuranceContractStat;
import io.github.mystagogy.insuranceinterface.domain.stat.entity.GenderType;
import io.github.mystagogy.insuranceinterface.domain.stat.repository.CarInsuranceContractStatRepository;
import java.math.BigDecimal;
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
class CarInsuranceContractStatServiceTest {

    @Mock
    private CarInsuranceContractStatRepository carInsuranceContractStatRepository;

    @Mock
    private ApiInfoRepository apiInfoRepository;

    @Mock
    private ApiCallHistoryAuditService apiCallHistoryAuditService;

    @Mock
    private CarInsuranceContractApiClient carInsuranceContractApiClient;

    @Mock
    private StatFailureResolver failureResolver;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CarInsuranceContractStatService carInsuranceContractStatService;

    @Test
    void requestAlwaysFetchesExternalDataEvenWhenStatsAlreadyExist() {
        ApiInfo apiInfo = new ApiInfo(CarInsuranceContractStatService.API_NAME, "금융위원회", "http://example.com", 5000);
        ApiCallHistory pendingHistory = new ApiCallHistory(apiInfo, null, "fromYm=202401,toYm=202401");
        ReflectionTestUtils.setField(pendingHistory, "id", 10L);
        CarInsuranceContractStat existing = new CarInsuranceContractStat(
            apiInfo,
            "202401",
            "개인용",
            "대인배상1",
            GenderType.MALE,
            "20대 이하",
            "외산",
            "중형",
            10L,
            new BigDecimal("10000.00"),
            "{}"
        );

        when(apiInfoRepository.findByApiNameAndUseYnTrue(CarInsuranceContractStatService.API_NAME)).thenReturn(Optional.of(apiInfo));
        when(apiCallHistoryAuditService.createPending(any(), any(), any())).thenReturn(pendingHistory);
        when(carInsuranceContractApiClient.fetch("202401", "202401")).thenReturn(List.of(
            new CarInsuranceContractExternalItem(
                "202401",
                "개인용",
                "대인배상1",
                GenderType.MALE,
                "20대 이하",
                "외산",
                "중형",
                10L,
                new BigDecimal("10000.00"),
                "{}"
            )
        ));
        when(carInsuranceContractStatRepository.findByBaseYmBetweenOrderByBaseYmAsc("202401", "202401"))
            .thenReturn(List.of(existing));

        CarInsuranceContractStatResponse response = carInsuranceContractStatService.getContractStats(
            new CarInsuranceContractStatQueryRequest("202401", "202401")
        );

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.items().get(0).insuranceType()).isEqualTo("개인용");
        verify(carInsuranceContractApiClient).fetch("202401", "202401");
        verify(carInsuranceContractStatRepository).upsertAll(any());
        verify(apiCallHistoryAuditService).recordSuccess(10L, 200, "fetched contract statistics");
    }

    @Test
    void fetchesExternalDataAndCompletesHistory() {
        ApiInfo apiInfo = new ApiInfo(CarInsuranceContractStatService.API_NAME, "금융위원회", "http://example.com", 5000);
        ApiCallHistory pendingHistory = new ApiCallHistory(apiInfo, null, "fromYm=202401,toYm=202401");
        ReflectionTestUtils.setField(pendingHistory, "id", 1L);
        CarInsuranceContractStat savedStat = new CarInsuranceContractStat(
            apiInfo,
            "202401",
            "개인용",
            "대인배상1",
            GenderType.MALE,
            "20대 이하",
            "외산",
            "중형",
            10L,
            new BigDecimal("10000.00"),
            "{}"
        );

        when(apiInfoRepository.findByApiNameAndUseYnTrue(CarInsuranceContractStatService.API_NAME)).thenReturn(Optional.of(apiInfo));
        when(apiCallHistoryAuditService.createPending(any(), any(), any())).thenReturn(pendingHistory);
        when(carInsuranceContractApiClient.fetch("202401", "202401")).thenReturn(List.of(
            new CarInsuranceContractExternalItem(
                "202401",
                "개인용",
                "대인배상1",
                GenderType.MALE,
                "20대 이하",
                "외산",
                "중형",
                10L,
                new BigDecimal("10000.00"),
                "{}"
            )
        ));
        when(carInsuranceContractStatRepository.findByBaseYmBetweenOrderByBaseYmAsc("202401", "202401"))
            .thenReturn(List.of(savedStat));

        CarInsuranceContractStatResponse response = carInsuranceContractStatService.getContractStats(
            new CarInsuranceContractStatQueryRequest("202401", "202401")
        );

        assertThat(response.totalCount()).isEqualTo(1);
        verify(carInsuranceContractStatRepository).upsertAll(any());
        verify(apiCallHistoryAuditService).recordSuccess(1L, 200, "fetched contract statistics");
    }

    /**
     * 외부 API 호출이 실패하면 FAIL 이력과 에러 로그를 함께 남겨야 한다.
     */
    @Test
    void externalFailureWritesErrorLogAndMarksHistoryFail() {
        ApiInfo apiInfo = new ApiInfo(CarInsuranceContractStatService.API_NAME, "금융위원회", "http://example.com", 5000);
        ApiCallHistory pendingHistory = new ApiCallHistory(apiInfo, null, "fromYm=202401,toYm=202401");
        ReflectionTestUtils.setField(pendingHistory, "id", 2L);

        when(apiInfoRepository.findByApiNameAndUseYnTrue(CarInsuranceContractStatService.API_NAME)).thenReturn(Optional.of(apiInfo));
        when(apiCallHistoryAuditService.createPending(any(), any(), any())).thenReturn(pendingHistory);
        when(carInsuranceContractApiClient.fetch("202401", "202401"))
            .thenThrow(new IllegalStateException("자동차보험 계약정보 API 응답이 비어 있습니다."));
        when(failureResolver.resolve(any(), any(), any(), any())).thenReturn(
            new StatFailureResolver.ResolvedFailure(
                500,
                "자동차보험 계약정보 외부 API 호출에 실패했습니다.",
                "car insurance contract api failed",
                io.github.mystagogy.insuranceinterface.domain.log.entity.ErrorType.BAD_RESPONSE,
                null
            )
        );

        assertThatThrownBy(() -> carInsuranceContractStatService.getContractStats(
            new CarInsuranceContractStatQueryRequest("202401", "202401")
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("자동차보험 계약정보 조회에 실패했습니다.");

        verify(apiCallHistoryAuditService).recordFailure(
            eq(2L),
            eq(apiInfo),
            eq(500),
            eq("자동차보험 계약정보 외부 API 호출에 실패했습니다."),
            eq("car insurance contract api failed"),
            any(),
            any()
        );
    }
}
