package io.github.mystagogy.insuranceinterface.domain.history.controller;

import io.github.mystagogy.insuranceinterface.common.response.ApiResponse;
import io.github.mystagogy.insuranceinterface.domain.history.dto.HistoryItemResponse;
import io.github.mystagogy.insuranceinterface.domain.history.dto.HistoryQueryRequest;
import io.github.mystagogy.insuranceinterface.domain.history.service.HistoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/recent")
    public ApiResponse<List<HistoryItemResponse>> getRecent(@Valid HistoryQueryRequest request) {
        return ApiResponse.ok(historyService.getRecent(request));
    }
}
