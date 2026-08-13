package com.yigit.requestms.request.entity;

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

// Append-only. There are no setters and no update path: a trail that can be
// edited is not a trail.
//
// Statuses are stored as plain strings rather than two enum columns because the
// table records both machines. A request moving NEW to PRIORITIZED and a task
// moving TESTING to IN_PROGRESS are the same kind of fact here, and the reports
// read them together.
@Entity
@Table(name = "YIGIT_REQUEST_STATUS_HISTORY")
public class RequestStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "yigitSeqHistory")
    @SequenceGenerator(name = "yigitSeqHistory",
            sequenceName = "YIGIT_SEQ_HISTORY", allocationSize = 1)
    @Column(name = "HISTORY_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "REQUEST_ID", nullable = false)
    private RequestEntity request;

    // Null for the creation event, which has nothing before it.
    @Column(name = "OLD_STATUS", length = 30)
    private String oldStatus;

    @Column(name = "NEW_STATUS", nullable = false, length = 30)
    private String newStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CHANGED_BY", nullable = false)
    private UserEntity changedBy;

    @Column(name = "CHANGED_AT", nullable = false)
    private LocalDateTime changedAt;

    protected RequestStatusHistoryEntity() {
    }

    public RequestStatusHistoryEntity(RequestEntity request, Enum<?> oldStatus,
                                      Enum<?> newStatus, UserEntity changedBy) {
        this.request = request;
        this.oldStatus = oldStatus == null ? null : oldStatus.name();
        this.newStatus = newStatus.name();
        this.changedBy = changedBy;
        this.changedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public RequestEntity getRequest() {
        return request;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public UserEntity getChangedBy() {
        return changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RequestStatusHistoryEntity that)) {
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
        return "RequestStatusHistoryEntity{" + oldStatus + " -> " + newStatus + "}";
    }
}