package com.yigit.requestms.user.entity;

import com.yigit.requestms.user.enums.Role;
import com.yigit.requestms.user.enums.SecurityQuestion;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "YIGIT_USERS")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "yigitSeqUsers")
    @SequenceGenerator(name = "yigitSeqUsers", sequenceName = "YIGIT_SEQ_USERS", allocationSize = 1)
    @Column(name = "USER_ID")
    private Long id;

    @Column(name = "NAME_SURNAME", nullable = false, length = 100)
    private String nameSurname;

    @Column(name = "EMAIL", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "PASSWORD_HASH", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 20)
    private Role role;

    // Soft delete. Users are never removed, so the foreign keys in workflows,
    // prioritizations and status history keep resolving after someone leaves.
    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean active = true;

    @Column(name = "MUST_CHANGE_PASSWORD", nullable = false)
    private boolean mustChangePassword = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "SECURITY_QUESTION", nullable = false, length = 100)
    private SecurityQuestion securityQuestion;

    @Column(name = "SECURITY_ANSWER_HASH", nullable = false, length = 255)
    private String securityAnswerHash;

    @Column(name = "FAILED_RESET_ATTEMPTS", nullable = false)
    private int failedResetAttempts = 0;

    // Not the same as inactive: locked is an automatic response to failed reset
    // attempts, inactive is an administrator's decision. Different fixes.
    @Column(name = "IS_LOCKED", nullable = false)
    private boolean locked = false;

    @Column(name = "PREFERRED_THEME", nullable = false, length = 10)
    private String preferredTheme = "light";

    @Column(name = "PREFERRED_LANGUAGE", nullable = false, length = 5)
    private String preferredLanguage = "tr";

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    protected UserEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getNameSurname() {
        return nameSurname;
    }

    public void setNameSurname(String nameSurname) {
        this.nameSurname = nameSurname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public SecurityQuestion getSecurityQuestion() {
        return securityQuestion;
    }

    public void setSecurityQuestion(SecurityQuestion securityQuestion) {
        this.securityQuestion = securityQuestion;
    }

    public String getSecurityAnswerHash() {
        return securityAnswerHash;
    }

    public void setSecurityAnswerHash(String securityAnswerHash) {
        this.securityAnswerHash = securityAnswerHash;
    }

    public int getFailedResetAttempts() {
        return failedResetAttempts;
    }

    public void setFailedResetAttempts(int failedResetAttempts) {
        this.failedResetAttempts = failedResetAttempts;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String getPreferredTheme() {
        return preferredTheme;
    }

    public void setPreferredTheme(String preferredTheme) {
        this.preferredTheme = preferredTheme;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Compare on the key alone: two loads of the same row must stay equal even
    // after one of them is edited.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserEntity that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "UserEntity{id=" + id + ", email='" + email + "', role=" + role + "}";
    }
}