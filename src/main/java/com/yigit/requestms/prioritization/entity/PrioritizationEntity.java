package com.yigit.requestms.prioritization.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "YIGIT_PRIORIZATIONS")
public class PrioritizationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "yigitSeqPriorizations")
    @SequenceGenerator(name = "yigitSeqPriorizations",
            sequenceName = "YIGIT_SEQ_PRIORIZATIONS", allocationSize = 1)
    @Column(name = "PRIORITY_ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "REQUEST_ID", nullable = false, unique = true)
    private RequestEntity request;

    @Column(name = "IMPACT", nullable = false)
    private int impact;

    @Column(name = "URGENCY", nullable = false)
    private int urgency;

    // Read-only: the column is virtual, computed by the database as
    // impact * urgency. Writing it from here would create a second source of
    // truth that could disagree with the stored value.
    @Column(name = "PRIORITY_SCORE", insertable = false, updatable = false)
    private Integer priorityScore;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PRIORITIZED_BY", nullable = false)
    private UserEntity prioritizedBy;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    protected PrioritizationEntity() {
    }

    public PrioritizationEntity(RequestEntity request, int impact, int urgency,
                                UserEntity prioritizedBy) {
        this.request = request;
        this.impact = impact;
        this.urgency = urgency;
        this.prioritizedBy = prioritizedBy;
        this.createdAt = LocalDateTime.now();
    }

    // Revising a score updates the existing row rather than replacing it, so
    // createdAt and prioritizedBy keep describing the original scoring event.
    public void revise(int impact, int urgency) {
        this.impact = impact;
        this.urgency = urgency;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public RequestEntity getRequest() {
        return request;
    }

    public int getImpact() {
        return impact;
    }

    public int getUrgency() {
        return urgency;
    }

    public Integer getPriorityScore() {
        return priorityScore;
    }

    public UserEntity getPrioritizedBy() {
        return prioritizedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PrioritizationEntity that)) {
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
        return "PrioritizationEntity{id=" + id + ", impact=" + impact
                + ", urgency=" + urgency + "}";
    }
}