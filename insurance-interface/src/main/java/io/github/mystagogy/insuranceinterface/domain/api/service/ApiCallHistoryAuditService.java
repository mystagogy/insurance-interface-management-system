package io.github.mystagogy.insuranceinterface.domain.api.service;

import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiCallHistory;
import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiInfo;
import io.github.mystagogy.insuranceinterface.domain.api.repository.ApiCallHistoryRepository;
import io.github.mystagogy.insuranceinterface.domain.auth.entity.User;
import io.github.mystagogy.insuranceinterface.domain.log.entity.ErrorLog;
import io.github.mystagogy.insuranceinterface.domain.log.entity.ErrorType;
import io.github.mystagogy.insuranceinterface.domain.log.repository.ErrorLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiCallHistoryAuditService {

    private final ApiCallHistoryRepository apiCallHistoryRepository;
    private final ErrorLogRepository errorLogRepository;

    public ApiCallHistoryAuditService(
        ApiCallHistoryRepository apiCallHistoryRepository,
        ErrorLogRepository errorLogRepository
    ) {
        this.apiCallHistoryRepository = apiCallHistoryRepository;
        this.errorLogRepository = errorLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiCallHistory recordCacheHit(
        ApiInfo apiInfo,
        User requestedBy,
        String requestParams,
        int statusCode,
        String responseSummary
    ) {
        ApiCallHistory history = new ApiCallHistory(apiInfo, requestedBy, requestParams);
        history.completeSuccess(statusCode, responseSummary);
        return apiCallHistoryRepository.save(history);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiCallHistory createPending(ApiInfo apiInfo, User requestedBy, String requestParams) {
        return apiCallHistoryRepository.save(new ApiCallHistory(apiInfo, requestedBy, requestParams));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(Long historyId, int statusCode, String responseSummary) {
        ApiCallHistory history = findHistory(historyId);
        history.completeSuccess(statusCode, responseSummary);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
        Long historyId,
        ApiInfo apiInfo,
        int statusCode,
        String errorMessage,
        String responseSummary,
        ErrorType errorType,
        String stackTrace
    ) {
        ApiCallHistory history = findHistory(historyId);
        history.completeFailure(statusCode, errorMessage, responseSummary);
        errorLogRepository.save(new ErrorLog(apiInfo, history, errorType, errorMessage, stackTrace));
    }

    private ApiCallHistory findHistory(Long historyId) {
        return apiCallHistoryRepository.findById(historyId)
            .orElseThrow(() -> new IllegalStateException("API 호출 이력을 찾을 수 없습니다."));
    }
}
