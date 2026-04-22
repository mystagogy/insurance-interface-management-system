package io.github.mystagogy.insuranceinterface.domain.stat.service;

import io.github.mystagogy.insuranceinterface.domain.log.entity.ErrorType;
import org.springframework.stereotype.Component;

@Component
public class CarInsuranceContractStatFailureResolver {

    private static final String SAFE_EXTERNAL_ERROR_MESSAGE = "자동차보험 계약정보 외부 API 호출에 실패했습니다.";

    /**
     * 예외를 호출 이력과 에러 로그에 저장할 수 있는 실패 정보로 변환한다.
     */
    public ResolvedFailure resolve(Exception exception) {
        return new ResolvedFailure(
            resolveStatusCode(exception),
            resolveMessage(exception),
            "car insurance contract api failed",
            resolveErrorType(exception),
            null
        );
    }

    private int resolveStatusCode(Exception exception) {
        return exception instanceof IllegalArgumentException ? 400 : 500;
    }

    private ErrorType resolveErrorType(Exception exception) {
        String message = exception.getMessage();
        if (message != null && message.toLowerCase().contains("timeout")) {
            return ErrorType.TIMEOUT;
        }
        if (message != null && message.toLowerCase().contains("connect")) {
            return ErrorType.CONNECTION;
        }
        if (message != null && (message.contains("응답") || message.contains("API 오류"))) {
            return ErrorType.BAD_RESPONSE;
        }
        return ErrorType.UNKNOWN;
    }

    private String resolveMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "자동차보험 계약정보 조회 처리 중 오류가 발생했습니다.";
        }
        if (exception instanceof IllegalArgumentException) {
            return message;
        }
        return SAFE_EXTERNAL_ERROR_MESSAGE;
    }

    public record ResolvedFailure(
        int statusCode,
        String errorMessage,
        String responseSummary,
        ErrorType errorType,
        String stackTrace
    ) {
    }
}
