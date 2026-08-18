package com.yigit.requestms.user.entity;

import com.yigit.requestms.user.enums.Role;
import com.yigit.requestms.user.enums.SecurityQuestion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

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

    // Null while the account is waiting to be set up. An administrator opens
    // the account; the person who will use it chooses what guards it.
    @Column(name = "PASSWORD_HASH", length = 255)
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

    // Chosen during setup, not by whoever opened the account: a question whose
    // answer an administrator already knows proves nothing about who is asking.
    @Enumerated(EnumType.STRING)
    @Column(name = "SECURITY_QUESTION", length = 100)
    private SecurityQuestion securityQuestion;

    @Column(name = "SECURITY_ANSWER_HASH", length = 255)
    private String securityAnswerHash;

    @Column(name = "FAILED_RESET_ATTEMPTS", nullable = false)
    private int failedResetAttempts = 0;

    // Not the same as inactive: locked is an automatic response to failed reset
    // attempts, inactive is an administrator's decision. Different fixes.
    @Column(name = "IS_LOCKED", nullable = false)
    private boolean locked = false;

    // A one-time code handed over in person, because there is no mail server to
    // send it through. Cleared the moment the account is set up, so a code that
    // has been used is a code that no longer opens anything.
    @Column(name = "SETUP_TOKEN", length = 64)
    private String setupToken;

    @Column(name = "SETUP_TOKEN_EXPIRES_AT")
    private LocalDateTime setupTokenExpiresAt;

    @Column(name = "PREFERRED_THEME", nullable = false, length = 10)
    private String preferredTheme = "light";

    @Column(name = "PREFERRED_LANGUAGE", nullable = false, length = 5)
    private String preferredLanguage = "tr";

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    protected UserEntity() {
    }

    // Opens an account nobody can sign into yet. The credentials arrive when
    // the person holding the setup code supplies them.
    public UserEntity(String nameSurname, String email, Role role,
                      String setupToken, LocalDateTime setupTokenExpiresAt) {
        this.nameSurname = nameSurname;
        this.email = email;
        this.role = role;
        this.setupToken = setupToken;
        this.setupTokenExpiresAt = setupTokenExpiresAt;
        this.createdAt = LocalDateTime.now();
    }

    // For the seeded accounts, which arrive already set up because there is
    // nobody to hand a code to when the schema is first filled.
    public UserEntity(String nameSurname, String email, String passwordHash, Role role,
                      SecurityQuestion securityQuestion, String securityAnswerHash) {
        this.nameSurname = nameSurname;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.securityQuestion = securityQuestion;
        this.securityAnswerHash = securityAnswerHash;
        this.createdAt = LocalDateTime.now();
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

    public String getSecurityAnswerHash() {
        return securityAnswerHash;
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

    public String getSetupToken() {
        return setupToken;
    }

    public LocalDateTime getSetupTokenExpiresAt() {
        return setupTokenExpiresAt;
    }

    // An account is waiting when it has a code and no password. The two are
    // mutually exclusive, which a database CHECK also enforces.
    public boolean isAwaitingSetup() {
        return setupToken != null;
    }

    public boolean isSetupTokenExpired() {
        return setupTokenExpiresAt != null && LocalDateTime.now().isAfter(setupTokenExpiresAt);
    }

    // Everything the account was missing arrives at once, and the code that
    // allowed it is destroyed in the same move: there is no window where both
    // the code and the password open the account.
    public void completeSetup(String passwordHash, SecurityQuestion question, String answerHash) {
        this.passwordHash = passwordHash;
        this.securityQuestion = question;
        this.securityAnswerHash = answerHash;
        this.setupToken = null;
        this.setupTokenExpiresAt = null;
        this.mustChangePassword = false;
        this.failedResetAttempts = 0;
        this.locked = false;
    }

    // Issued again when the first code expires or goes astray. The old one
    // stops working because it is overwritten, not because it is remembered.
    public void reissueSetupToken(String token, LocalDateTime expiresAt) {
        this.passwordHash = null;
        this.securityQuestion = null;
        this.securityAnswerHash = null;
        this.setupToken = token;
        this.setupTokenExpiresAt = expiresAt;
        this.mustChangePassword = false;
        this.failedResetAttempts = 0;
        this.locked = false;
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