package com.yigit.requestms.request.entity;

import com.yigit.requestms.request.enums.RequestStatus;
import com.yigit.requestms.request.exception.InvalidRequestTransitionException;
import com.yigit.requestms.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "YIGIT_REQUESTS")
public class RequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REQUEST_ID")
    private Long id;

    // Lazy: list screens read the customer name through a DTO projection in a
    // single query. Eager here would bring back the N+1 that projection avoids.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CUSTOMER_ID", nullable = false)
    private UserEntity customer;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    // Character Large Object rather than VARCHAR2: free text can run past the
    // 4000 byte ceiling. The cost is that it cannot be indexed and is fetched
    // separately, which is why list projections leave it out.
    @Lob
    @Column(name = "DESCRIPTION", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 30)
    private RequestStatus status = RequestStatus.NEW;

    @Column(name = "REJECTION_REASON", length = 500)
    private String rejectionReason;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "CLOSED_AT")
    private LocalDateTime closedAt;

    protected RequestEntity() {
    }

    public RequestEntity(UserEntity customer, String title, String description) {
        this.customer = customer;
        this.title = title;
        this.description = description;
        this.status = RequestStatus.NEW;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public UserEntity getCustomer() {
        return customer;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    // No setStatus by design: every status change goes through the state
    // machine, so an illegal transition cannot be written by accident.
    public void transitionTo(RequestStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidRequestTransitionException(status, target);
        }
        this.status = target;
    }

    public void markRejected(String reason) {
        transitionTo(RequestStatus.REJECTED);
        this.rejectionReason = reason;
    }

    public void markClosed(LocalDateTime closedAt) {
        transitionTo(RequestStatus.CLOSED);
        this.closedAt = closedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RequestEntity that)) {
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
        return "RequestEntity{id=" + id + ", status=" + status + "}";
    }
}