package io.github.mystagogy.insuranceinterface.domain.retry.controller;

import io.github.mystagogy.insuranceinterface.common.response.ApiResponse;
import io.github.mystagogy.insuranceinterface.domain.retry.dto.RetryResultResponse;
import io.github.mystagogy.insuranceinterface.domain.retry.service.RetryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/retry")
public class RetryController {

    private final RetryService retryService;

    public RetryController(RetryService retryService) {
        this.retryService = retryService;
    }

    @PostMapping
    public ApiResponse<RetryResultResponse> retry(@RequestParam String requestId) {
        return ApiResponse.ok(retryService.retry(requestId));
    }
}
