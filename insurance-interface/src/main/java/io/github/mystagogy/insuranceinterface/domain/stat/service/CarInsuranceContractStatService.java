package io.github.mystagogy.insuranceinterface.domain.stat.service;

import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiCallHistory;
import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiInfo;
import io.github.mystagogy.insuranceinterface.domain.api.repository.ApiInfoRepository;
import io.github.mystagogy.insuranceinterface.domain.api.service.ApiCallHistoryAuditService;
import io.github.mystagogy.insuranceinterface.domain.auth.entity.User;
import io.github.mystagogy.insuranceinterface.domain.auth.repository.UserRepository;
import io.github.mystagogy.insuranceinterface.domain.stat.client.CarInsuranceContractApiClient;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.CarInsuranceContractStatItemResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.CarInsuranceContractStatQueryRequest;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.CarInsuranceContractStatResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.external.CarInsuranceContractExternalItem;
import io.github.mystagogy.insuranceinterface.domain.stat.entity.CarInsuranceContractStat;
import io.github.mystagogy.insuranceinterface.domain.stat.repository.CarInsuranceContractStatRepository;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CarInsuranceContractStatService {

    public static final String API_NAME = "CAR_INSURANCE_CONTRACT";

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final CarInsuranceContractStatRepository carInsuranceContractStatRepository;
    private final ApiInfoRepository apiInfoRepository;
    private final ApiCallHistoryAuditService apiCallHistoryAuditService;
    private final CarInsuranceContractApiClient carInsuranceContractApiClient;
    private final CarInsuranceContractStatFailureResolver failureResolver;
    private final UserRepository userRepository;

    public CarInsuranceContractStatService(
        CarInsuranceContractStatRepository carInsuranceContractStatRepository,
        ApiInfoRepository apiInfoRepository,
        ApiCallHistoryAuditService apiCallHistoryAuditService,
        CarInsuranceContractApiClient carInsuranceContractApiClient,
        CarInsuranceContractStatFailureResolver failureResolver,
        UserRepository userRepository
    ) {
        this.carInsuranceContractStatRepository = carInsuranceContractStatRepository;
        this.apiInfoRepository = apiInfoRepository;
        this.apiCallHistoryAuditService = apiCallHistoryAuditService;
        this.carInsuranceContractApiClient = carInsuranceContractApiClient;
        this.failureResolver = failureResolver;
        this.userRepository = userRepository;
    }

    /**
     * 자동차보험 계약 통계를 조회한다.
     * 항상 외부 API를 호출해 최신 데이터를 동기화한 뒤, 저장된 통계를 응답으로 반환한다.
     */
    public CarInsuranceContractStatResponse getContractStats(CarInsuranceContractStatQueryRequest request) {
        validatePeriod(request);

        RequestContext context = createRequestContext(request);

        ApiCallHistory history = apiCallHistoryAuditService.createPending(
            context.apiInfo(),
            context.requestedBy(),
            context.requestParams()
        );

        try {
            synchronizeStats(request, context.apiInfo());
            apiCallHistoryAuditService.recordSuccess(
                history.getId(),
                200,
                "fetched contract statistics"
            );
        } catch (Exception exception) {
            handleFailure(history, context.apiInfo(), exception);
        }

        return toResponse(request, loadStats(request.fromYm(), request.toYm()));
    }

    /**
     * 조회 기간이 yyyyMM 형식인지, 시작월이 종료월보다 늦지 않은지 검증한다.
     */
    private void validatePeriod(CarInsuranceContractStatQueryRequest request) {
        try {
            YearMonth from = YearMonth.parse(request.fromYm(), YEAR_MONTH_FORMATTER);
            YearMonth to = YearMonth.parse(request.toYm(), YEAR_MONTH_FORMATTER);
            if (from.isAfter(to)) {
                throw new IllegalArgumentException("조회 시작년월은 종료년월보다 늦을 수 없습니다.");
            }
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("조회 기간은 yyyyMM 형식이어야 합니다.");
        }
    }

    private List<CarInsuranceContractStat> loadStats(String fromYm, String toYm) {
        return carInsuranceContractStatRepository.findByBaseYmBetweenOrderByBaseYmAsc(fromYm, toYm);
    }

    /**
     * 요청 처리에 필요한 API 설정, 요청 사용자, 로그용 파라미터를 한 번에 구성한다.
     */
    private RequestContext createRequestContext(CarInsuranceContractStatQueryRequest request) {
        ApiInfo apiInfo = apiInfoRepository.findByApiNameAndUseYnTrue(API_NAME)
            .orElseThrow(() -> new IllegalStateException("자동차보험 계약정보 API 설정을 찾을 수 없습니다."));
        return new RequestContext(apiInfo, currentUserOrNull(), formatRequestParams(request));
    }

    /**
     * 외부 API에서 기간 데이터를 조회하고 내부 통계 테이블에 upsert 한다.
     */
    private void synchronizeStats(CarInsuranceContractStatQueryRequest request, ApiInfo apiInfo) {
        List<CarInsuranceContractExternalItem> externalItems =
            carInsuranceContractApiClient.fetch(request.fromYm(), request.toYm());
        upsertStats(apiInfo, externalItems);
    }

    /**
     * 예외를 운영용 실패 정보로 변환해 호출 이력과 에러 로그를 남긴 뒤 공통 예외로 감싼다.
     */
    private void handleFailure(ApiCallHistory history, ApiInfo apiInfo, Exception exception) {
        CarInsuranceContractStatFailureResolver.ResolvedFailure failure = failureResolver.resolve(exception);
        apiCallHistoryAuditService.recordFailure(
            history.getId(),
            apiInfo,
            failure.statusCode(),
            failure.errorMessage(),
            failure.responseSummary(),
            failure.errorType(),
            failure.stackTrace()
        );
        throw new IllegalStateException("자동차보험 계약정보 조회에 실패했습니다.", exception);
    }

    /**
     * 외부 응답을 내부 엔티티로 변환한 뒤 DB upsert 를 수행한다.
     */
    private void upsertStats(ApiInfo apiInfo, List<CarInsuranceContractExternalItem> externalItems) {
        List<CarInsuranceContractStat> stats = externalItems.stream()
            .map(externalItem -> new CarInsuranceContractStat(
                apiInfo,
                externalItem.baseYm(),
                defaultText(externalItem.insuranceType(), "미상"),
                defaultText(externalItem.coverageType(), "미상"),
                externalItem.gender(),
                defaultText(externalItem.ageGroup(), "미상"),
                defaultText(externalItem.carOriginType(), "미상"),
                defaultText(externalItem.carType(), "미상"),
                externalItem.contractCount(),
                externalItem.earnedPremium(),
                externalItem.rawData()
            ))
            .toList();
        if (!stats.isEmpty()) {
            carInsuranceContractStatRepository.upsertAll(stats);
        }
    }

    /**
     * 저장된 엔티티 목록을 API 응답 DTO 구조로 변환한다.
     */
    private CarInsuranceContractStatResponse toResponse(
        CarInsuranceContractStatQueryRequest request,
        List<CarInsuranceContractStat> stats
    ) {
        List<CarInsuranceContractStatItemResponse> items = stats.stream()
            .map(stat -> new CarInsuranceContractStatItemResponse(
                stat.getBaseYm(),
                stat.getInsuranceType(),
                stat.getCoverageType(),
                stat.getGender().getDisplayName(),
                stat.getAgeGroup(),
                stat.getCarOriginType(),
                stat.getCarType(),
                stat.getContractCount(),
                stat.getEarnedPremium()
            ))
            .toList();
        return new CarInsuranceContractStatResponse(request.fromYm(), request.toYm(), items.size(), items);
    }

    /**
     * 현재 로그인 사용자를 조회하고, 인증 정보가 없으면 null 을 반환한다.
     */
    private User currentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userRepository.findByLoginIdAndUseYnTrue(authentication.getName()).orElse(null);
    }

    /**
     * 호출 이력 저장용 요청 파라미터 문자열을 생성한다.
     */
    private String formatRequestParams(CarInsuranceContractStatQueryRequest request) {
        return "fromYm=" + request.fromYm() + ",toYm=" + request.toYm();
    }

    /**
     * 외부 응답의 빈 문자열이나 null 을 기본값으로 치환한다.
     */
    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private record RequestContext(ApiInfo apiInfo, User requestedBy, String requestParams) {
    }
}
