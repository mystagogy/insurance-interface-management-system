package io.github.mystagogy.insuranceinterface.domain.stat.service;

import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiCallHistory;
import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiInfo;
import io.github.mystagogy.insuranceinterface.domain.api.repository.ApiInfoRepository;
import io.github.mystagogy.insuranceinterface.domain.api.service.ApiCallHistoryAuditService;
import io.github.mystagogy.insuranceinterface.domain.auth.repository.UserRepository;
import io.github.mystagogy.insuranceinterface.domain.stat.client.IndemnityInsuranceApiClient;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.IndemnityInsuranceStatItemResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.IndemnityInsuranceStatQueryRequest;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.IndemnityInsuranceStatResponse;
import io.github.mystagogy.insuranceinterface.domain.stat.dto.external.IndemnityInsuranceExternalItem;
import io.github.mystagogy.insuranceinterface.domain.stat.entity.IndemnityInsuranceStat;
import io.github.mystagogy.insuranceinterface.domain.stat.repository.IndemnityInsuranceStatRepository;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IndemnityInsuranceStatService extends AbstractAuditedStatService {

    public static final String API_NAME = "INDEMNITY_INSURANCE_SUBSCRIPTION";

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String SAFE_EXTERNAL_ERROR_MESSAGE = "실손보험 가입정보 외부 API 호출에 실패했습니다.";
    private static final String FALLBACK_ERROR_MESSAGE = "실손보험 가입정보 조회 처리 중 오류가 발생했습니다.";
    private static final String FAILURE_SUMMARY = "indemnity insurance subscription api failed";

    private final IndemnityInsuranceStatRepository indemnityInsuranceStatRepository;
    private final IndemnityInsuranceApiClient indemnityInsuranceApiClient;
    private final StatFailureResolver failureResolver;

    public IndemnityInsuranceStatService(
        IndemnityInsuranceStatRepository indemnityInsuranceStatRepository,
        ApiInfoRepository apiInfoRepository,
        ApiCallHistoryAuditService apiCallHistoryAuditService,
        IndemnityInsuranceApiClient indemnityInsuranceApiClient,
        StatFailureResolver failureResolver,
        UserRepository userRepository
    ) {
        super(apiInfoRepository, apiCallHistoryAuditService, userRepository);
        this.indemnityInsuranceStatRepository = indemnityInsuranceStatRepository;
        this.indemnityInsuranceApiClient = indemnityInsuranceApiClient;
        this.failureResolver = failureResolver;
    }

    /**
     * 실손보험 가입 통계를 조회한다.
     * 항상 외부 API를 호출해 최신 데이터를 동기화한 뒤, 저장된 통계를 응답으로 반환한다.
     */
    public IndemnityInsuranceStatResponse getSubscriptionStats(IndemnityInsuranceStatQueryRequest request) {
        validatePeriod(request);
        return executeWithAudit(
            API_NAME,
            "실손보험 가입정보 API 설정을 찾을 수 없습니다.",
            formatRequestParams(request),
            "fetched indemnity insurance subscription statistics",
            apiInfo -> {
                synchronizeStats(request, apiInfo);
                return toResponse(request, loadStats(request, apiInfo));
            },
            this::handleFailure
        );
    }

    /**
     * 조회 기간이 yyyyMM 형식인지, 시작년월이 종료년월보다 늦지 않은지 검증한다.
     */
    private void validatePeriod(IndemnityInsuranceStatQueryRequest request) {
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

    private List<IndemnityInsuranceStat> loadStats(IndemnityInsuranceStatQueryRequest request, ApiInfo apiInfo) {
        YearMonth from = YearMonth.parse(request.fromYm(), YEAR_MONTH_FORMATTER);
        YearMonth to = YearMonth.parse(request.toYm(), YEAR_MONTH_FORMATTER);
        return indemnityInsuranceStatRepository.findStats(
            apiInfo,
            from.atDay(1),
            to.atEndOfMonth()
        );
    }

    /**
     * 외부 API에서 기간 데이터를 조회하고 내부 통계 테이블에 upsert 한다.
     */
    private void synchronizeStats(IndemnityInsuranceStatQueryRequest request, ApiInfo apiInfo) {
        List<IndemnityInsuranceExternalItem> externalItems =
            indemnityInsuranceApiClient.fetch(request.fromYm(), request.toYm());
        upsertStats(apiInfo, externalItems);
    }

    /**
     * 예외를 운영용 실패 정보로 변환해 호출 이력과 에러 로그를 남긴 뒤 공통 예외로 감싼다.
     */
    private RuntimeException handleFailure(ApiCallHistory history, ApiInfo apiInfo, Exception exception) {
        StatFailureResolver.ResolvedFailure failure = failureResolver.resolve(
            exception,
            SAFE_EXTERNAL_ERROR_MESSAGE,
            FALLBACK_ERROR_MESSAGE,
            FAILURE_SUMMARY
        );
        apiCallHistoryAuditService.recordFailure(
            history.getId(),
            apiInfo,
            failure.statusCode(),
            failure.errorMessage(),
            failure.responseSummary(),
            failure.errorType(),
            failure.stackTrace()
        );
        return new IllegalStateException("실손보험 가입정보 조회에 실패했습니다.", exception);
    }

    /**
     * 외부 응답을 내부 엔티티로 변환한 뒤 DB upsert 를 수행한다.
     */
    private void upsertStats(ApiInfo apiInfo, List<IndemnityInsuranceExternalItem> externalItems) {
        List<IndemnityInsuranceStat> stats = externalItems.stream()
            .map(externalItem -> new IndemnityInsuranceStat(
                apiInfo,
                externalItem.statDate(),
                defaultText(externalItem.ageGroup(), "미상"),
                externalItem.gender(),
                defaultText(externalItem.indemnityType(), "미상"),
                defaultText(externalItem.coverageItem(), "미상"),
                externalItem.premiumAmount(),
                externalItem.rawData()
            ))
            .toList();
        if (!stats.isEmpty()) {
            indemnityInsuranceStatRepository.upsertAll(stats);
        }
    }

    /**
     * 저장된 엔티티 목록을 API 응답 DTO 구조로 변환한다.
     */
    private IndemnityInsuranceStatResponse toResponse(
        IndemnityInsuranceStatQueryRequest request,
        List<IndemnityInsuranceStat> stats
    ) {
        List<IndemnityInsuranceStatItemResponse> items = stats.stream()
            .map(stat -> new IndemnityInsuranceStatItemResponse(
                stat.getStatDate(),
                stat.getAgeGroup(),
                stat.getGender().getDisplayName(),
                stat.getIndemnityType(),
                stat.getCoverageItem(),
                stat.getPremiumAmount()
            ))
            .toList();
        return new IndemnityInsuranceStatResponse(request.fromYm(), request.toYm(), items.size(), items);
    }

    /**
     * 호출 이력 저장용 요청 파라미터 문자열을 생성한다.
     */
    private String formatRequestParams(IndemnityInsuranceStatQueryRequest request) {
        return "fromYm=" + request.fromYm() + ",toYm=" + request.toYm();
    }

}
