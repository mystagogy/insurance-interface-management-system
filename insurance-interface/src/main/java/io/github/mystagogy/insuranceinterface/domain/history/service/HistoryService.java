package io.github.mystagogy.insuranceinterface.domain.history.service;

import io.github.mystagogy.insuranceinterface.domain.history.dto.HistoryItemResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HistoryService {

    public List<HistoryItemResponse> getRecent() {
        return List.of(
            new HistoryItemResponse("REQ-20260420-001", "SUCCESS"),
            new HistoryItemResponse("REQ-20260420-002", "FAILED")
        );
    }
}

