package io.github.mystagogy.insuranceinterface.domain.api.repository;

import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiCallHistory;
import io.github.mystagogy.insuranceinterface.domain.api.entity.CallStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiCallHistoryRepository extends JpaRepository<ApiCallHistory, Long> {

    long countByRequestTimeGreaterThanEqualAndRequestTimeLessThan(LocalDateTime start, LocalDateTime end);

    long countByRequestTimeGreaterThanEqualAndRequestTimeLessThanAndCallStatus(
        LocalDateTime start,
        LocalDateTime end,
        CallStatus callStatus
    );

    @EntityGraph(attributePaths = "apiInfo")
    List<ApiCallHistory> findByRequestTimeGreaterThanEqualAndRequestTimeLessThanOrderByRequestTimeDesc(
        LocalDateTime start,
        LocalDateTime end
    );

    @EntityGraph(attributePaths = "apiInfo")
    List<ApiCallHistory> findTop50ByOrderByRequestTimeDesc();
}
