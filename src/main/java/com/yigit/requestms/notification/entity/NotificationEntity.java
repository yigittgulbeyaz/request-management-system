package com.yigit.requestms.notification.entity;

import com.yigit.requestms.request.entity.RequestEntity;
import com.yigit.requestms.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "YIGIT_NOTIFICATIONS")
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "yigitSeqNotifications")
    @SequenceGenerator(name = "yigitSeqNotifications",
            sequenceName = "YIGIT_SEQ_NOTIFICATIONS", allocationSize = 1)
    @Column(name = "NOTIFICATION_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity recipient;

    @Column(name = "MESSAGE", nullable = false, length = 255)
    private String message;

    @Column(name = "IS_READ", nullable = false)
    private boolean read = false;

    // Lets the reader click through to what the notification is about.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RELATED_REQUEST_ID")
    private RequestEntity relatedRequest;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    protected NotificationEntity() {
    }

    public NotificationEntity(UserEntity recipient, String message, RequestEntity relatedRequest) {
        this.recipient = recipient;
        this.message = message;
        this.relatedRequest = relatedRequest;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public UserEntity getRecipient() {
        return recipient;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public RequestEntity getRelatedRequest() {
        return relatedRequest;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void markRead() {
        this.read = true;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof NotificationEntity that)) {
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
        return "NotificationEntity{id=" + id + ", read=" + read + "}";
    }
}