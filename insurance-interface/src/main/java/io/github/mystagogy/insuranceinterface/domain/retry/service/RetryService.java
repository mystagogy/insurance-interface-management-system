package io.github.mystagogy.insuranceinterface.domain.retry.service;

import io.github.mystagogy.insuranceinterface.domain.retry.dto.RetryResultResponse;
import org.springframework.stereotype.Service;

@Service
public class RetryService {

    public RetryResultResponse retry(String requestId) {
        return new RetryResultResponse(requestId, true);
    }
}

