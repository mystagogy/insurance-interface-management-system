package io.github.mystagogy.insuranceinterface.domain.log.repository;

import io.github.mystagogy.insuranceinterface.domain.log.entity.ErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {
}
