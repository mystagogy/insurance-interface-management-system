package io.github.mystagogy.insuranceinterface.domain.stat.service;

import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiCallHistory;
import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiInfo;
import io.github.mystagogy.insuranceinterface.domain.api.repository.ApiInfoRepository;
import io.github.mystagogy.insuranceinterface.domain.api.service.ApiCallHistoryAuditService;
import io.github.mystagogy.insuranceinterface.domain.auth.entity.User;
import io.github.mystagogy.insuranceinterface.domain.auth.repository.UserRepository;
import io.github.mystagogy.insuranceinterface.domain.stat.client.LifeInsuranceApiClient;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.LifeInsuranceStatItemResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.LifeInsuranceStatQueryRequest;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.LifeInsuranceStatResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.external.LifeInsuranceExternalItem;
import io.github.mystagogy.insuranceinterface.domain.stat.entity.LifeInsuranceStat;
import io.github.mystagogy.insuranceinterface.domain.stat.repository.LifeInsuranceStatRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LifeInsuranceStatService {

    public static final String API_NAME = "LIFE_INSURANCE_SUBSCRIPTION";

    private final LifeInsuranceStatRepository lifeInsuranceStatRepository;
    private final ApiInfoRepository apiInfoRepository;
    private final ApiCallHistoryAuditService apiCallHistoryAuditService;
    private final LifeInsuranceApiClient lifeInsuranceApiClient;
    private final LifeInsuranceStatFailureResolver failureResolver;
    private final UserRepository userRepository;

    public LifeInsuranceStatService(
        LifeInsuranceStatRepository lifeInsuranceStatRepository,
        ApiInfoRepository apiInfoRepository,
        ApiCallHistoryAuditService apiCallHistoryAuditService,
        LifeInsuranceApiClient lifeInsuranceApiClient,
        LifeInsuranceStatFailureResolver failureResolver,
        UserRepository userRepository
    ) {
        this.lifeInsuranceStatRepository = lifeInsuranceStatRepository;
        this.apiInfoRepository = apiInfoRepository;
        this.apiCallHistoryAuditService = apiCallHistoryAuditService;
        this.lifeInsuranceApiClient = lifeInsuranceApiClient;
        this.failureResolver = failureResolver;
        this.userRepository = userRepository;
    }

    /**
     * 생명보험 가입 통계를 조회한다.
     * 항상 외부 API를 호출해 최신 데이터를 동기화한 뒤, 저장된 통계를 응답으로 반환한다.
     */
    public LifeInsuranceStatResponse getSubscriptionStats(LifeInsuranceStatQueryRequest request) {
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
                "fetched life insurance subscription statistics"
            );
        } catch (Exception exception) {
            handleFailure(history, context.apiInfo(), exception);
        }

        return toResponse(request, loadStats(request, context.apiInfo()));
    }

    /**
     * 조회 기간이 yyyy 형식인지, 시작년도가 종료년도보다 늦지 않은지 검증한다.
     */
    private void validatePeriod(LifeInsuranceStatQueryRequest request) {
        int from = parseYear(request.fromYear());
        int to = parseYear(request.toYear());
        if (from > to) {
            throw new IllegalArgumentException("조회 시작년도는 종료년도보다 늦을 수 없습니다.");
        }
    }

    private int parseYear(String year) {
        try {
            return Integer.parseInt(year);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("조회 기간은 yyyy 형식이어야 합니다.");
        }
    }

    private List<LifeInsuranceStat> loadStats(LifeInsuranceStatQueryRequest request, ApiInfo apiInfo) {
        LocalDate fromDate = LocalDate.of(Integer.parseInt(request.fromYear()), 1, 1);
        LocalDate toDate = LocalDate.of(Integer.parseInt(request.toYear()), 12, 31);
        return lifeInsuranceStatRepository.findStats(
            apiInfo,
            fromDate,
            toDate
        );
    }

    /**
     * 요청 처리에 필요한 API 설정, 요청 사용자, 로그용 파라미터를 한 번에 구성한다.
     */
    private RequestContext createRequestContext(LifeInsuranceStatQueryRequest request) {
        ApiInfo apiInfo = apiInfoRepository.findByApiNameAndUseYnTrue(API_NAME)
            .orElseThrow(() -> new IllegalStateException("생명보험 가입정보 API 설정을 찾을 수 없습니다."));
        return new RequestContext(apiInfo, currentUserOrNull(), formatRequestParams(request));
    }

    /**
     * 외부 API에서 기간 데이터를 조회하고 내부 통계 테이블에 upsert 한다.
     */
    private void synchronizeStats(LifeInsuranceStatQueryRequest request, ApiInfo apiInfo) {
        List<LifeInsuranceExternalItem> externalItems =
            lifeInsuranceApiClient.fetch(request.fromYear(), request.toYear());
        upsertStats(apiInfo, externalItems);
    }

    /**
     * 예외를 운영용 실패 정보로 변환해 호출 이력과 에러 로그를 남긴 뒤 공통 예외로 감싼다.
     */
    private void handleFailure(ApiCallHistory history, ApiInfo apiInfo, Exception exception) {
        LifeInsuranceStatFailureResolver.ResolvedFailure failure = failureResolver.resolve(exception);
        apiCallHistoryAuditService.recordFailure(
            history.getId(),
            apiInfo,
            failure.statusCode(),
            failure.errorMessage(),
            failure.responseSummary(),
            failure.errorType(),
            failure.stackTrace()
        );
        throw new IllegalStateException("생명보험 가입정보 조회에 실패했습니다.", exception);
    }

    /**
     * 외부 응답을 내부 엔티티로 변환한 뒤 DB upsert 를 수행한다.
     */
    private void upsertStats(ApiInfo apiInfo, List<LifeInsuranceExternalItem> externalItems) {
        List<LifeInsuranceStat> stats = externalItems.stream()
            .map(externalItem -> new LifeInsuranceStat(
                apiInfo,
                externalItem.statDate(),
                defaultText(externalItem.areaName(), "미상"),
                defaultText(externalItem.ageGroup(), "미상"),
                externalItem.gender(),
                defaultText(externalItem.insuranceType(), "미상"),
                externalItem.subscriptionCount(),
                externalItem.subscriptionRate(),
                externalItem.rawData()
            ))
            .toList();
        if (!stats.isEmpty()) {
            lifeInsuranceStatRepository.upsertAll(stats);
        }
    }

    /**
     * 저장된 엔티티 목록을 API 응답 DTO 구조로 변환한다.
     */
    private LifeInsuranceStatResponse toResponse(
        LifeInsuranceStatQueryRequest request,
        List<LifeInsuranceStat> stats
    ) {
        List<LifeInsuranceStatItemResponse> items = stats.stream()
            .map(stat -> new LifeInsuranceStatItemResponse(
                stat.getStatDate(),
                stat.getAreaName(),
                stat.getAgeGroup(),
                stat.getGender().getDisplayName(),
                stat.getInsuranceType(),
                stat.getSubscriptionCount(),
                stat.getSubscriptionRate()
            ))
            .toList();
        return new LifeInsuranceStatResponse(request.fromYear(), request.toYear(), items.size(), items);
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
    private String formatRequestParams(LifeInsuranceStatQueryRequest request) {
        return "fromYear=" + request.fromYear() + ",toYear=" + request.toYear();
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
