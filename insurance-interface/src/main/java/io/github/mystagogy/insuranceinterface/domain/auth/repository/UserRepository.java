package io.github.mystagogy.insuranceinterface.domain.auth.repository;

import io.github.mystagogy.insuranceinterface.domain.auth.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByLoginIdAndUseYnTrue(String loginId);
}
