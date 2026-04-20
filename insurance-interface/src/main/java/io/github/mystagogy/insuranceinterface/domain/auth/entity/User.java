package io.github.mystagogy.insuranceinterface.domain.auth.entity;

import io.github.mystagogy.insuranceinterface.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "app_user",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_app_user_login_id", columnNames = "login_id")
    }
)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "login_id", nullable = false, length = 50)
    private String loginId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Column(name = "use_yn", nullable = false)
    private boolean useYn;

    protected User() {
    }

    public User(String loginId, String passwordHash, String userName, UserRole role) {
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.userName = userName;
        this.role = role;
        this.useYn = true;
    }

    public Long getId() {
        return id;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getUserName() {
        return userName;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isUseYn() {
        return useYn;
    }

    public void deactivate() {
        this.useYn = false;
    }
}
