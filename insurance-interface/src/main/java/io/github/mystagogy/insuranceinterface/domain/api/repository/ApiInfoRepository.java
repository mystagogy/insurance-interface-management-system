package io.github.mystagogy.insuranceinterface.domain.api.repository;

import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiInfo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiInfoRepository extends JpaRepository<ApiInfo, Long> {

    Optional<ApiInfo> findByApiName(String apiName);

    Optional<ApiInfo> findByApiNameAndUseYnTrue(String apiName);
}
