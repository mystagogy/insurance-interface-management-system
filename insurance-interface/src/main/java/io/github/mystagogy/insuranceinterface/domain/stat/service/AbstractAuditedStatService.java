package io.github.mystagogy.insuranceinterface.domain.stat.service;

import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiCallHistory;
import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiInfo;
import io.github.mystagogy.insuranceinterface.domain.api.repository.ApiInfoRepository;
import io.github.mystagogy.insuranceinterface.domain.api.service.ApiCallHistoryAuditService;
import io.github.mystagogy.insuranceinterface.domain.auth.entity.User;
import io.github.mystagogy.insuranceinterface.domain.auth.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public abstract class AbstractAuditedStatService {

    private final ApiInfoRepository apiInfoRepository;
    protected final ApiCallHistoryAuditService apiCallHistoryAuditService;
    private final UserRepository userRepository;

    protected AbstractAuditedStatService(
        ApiInfoRepository apiInfoRepository,
        ApiCallHistoryAuditService apiCallHistoryAuditService,
        UserRepository userRepository
    ) {
        this.apiInfoRepository = apiInfoRepository;
        this.apiCallHistoryAuditService = apiCallHistoryAuditService;
        this.userRepository = userRepository;
    }

    protected <T> T executeWithAudit(
        String apiName,
        String apiNotFoundMessage,
        String requestParams,
        String successSummary,
        AuditedWork<T> work,
        FailureHandler failureHandler
    ) {
        ApiInfo apiInfo = findApiInfo(apiName, apiNotFoundMessage);
        ApiCallHistory history = apiCallHistoryAuditService.createPending(
            apiInfo,
            currentUserOrNull(),
            requestParams
        );

        try {
            T result = work.execute(apiInfo);
            apiCallHistoryAuditService.recordSuccess(history.getId(), 200, successSummary);
            return result;
        } catch (Exception exception) {
            throw failureHandler.handle(history, apiInfo, exception);
        }
    }

    protected String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    protected final ApiInfo findApiInfo(String apiName, String apiNotFoundMessage) {
        return apiInfoRepository.findByApiNameAndUseYnTrue(apiName)
            .orElseThrow(() -> new IllegalStateException(apiNotFoundMessage));
    }

    private User currentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userRepository.findByLoginIdAndUseYnTrue(authentication.getName()).orElse(null);
    }

    @FunctionalInterface
    protected interface AuditedWork<T> {
        T execute(ApiInfo apiInfo) throws Exception;
    }

    @FunctionalInterface
    protected interface FailureHandler {
        RuntimeException handle(ApiCallHistory history, ApiInfo apiInfo, Exception exception);
    }
}
