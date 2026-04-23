package io.github.mystagogy.insuranceinterface.domain.stat.service;

import io.github.mystagogy.insuranceinterface.domain.log.entity.ErrorType;
import org.springframework.stereotype.Component;

@Component
public class StatFailureResolver {

    /**
     * 예외를 호출 이력과 에러 로그에 저장할 수 있는 실패 정보로 변환한다.
     */
    public ResolvedFailure resolve(
        Exception exception,
        String safeExternalErrorMessage,
        String fallbackErrorMessage,
        String responseSummary
    ) {
        return new ResolvedFailure(
            resolveStatusCode(exception),
            resolveMessage(exception, safeExternalErrorMessage, fallbackErrorMessage),
            responseSummary,
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

    private String resolveMessage(
        Exception exception,
        String safeExternalErrorMessage,
        String fallbackErrorMessage
    ) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return fallbackErrorMessage;
        }
        if (exception instanceof IllegalArgumentException) {
            return message;
        }
        return safeExternalErrorMessage;
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
