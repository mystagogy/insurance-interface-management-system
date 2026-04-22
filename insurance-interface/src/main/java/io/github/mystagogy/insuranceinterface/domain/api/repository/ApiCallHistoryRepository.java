package io.github.mystagogy.insuranceinterface.domain.api.repository;

import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiCallHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiCallHistoryRepository extends JpaRepository<ApiCallHistory, Long> {
}
