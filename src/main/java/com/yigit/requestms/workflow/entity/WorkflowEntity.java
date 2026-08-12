package com.yigit.requestms.workflow.entity;

import com.yigit.requestms.request.entity.RequestEntity;
import com.yigit.requestms.user.entity.UserEntity;
import com.yigit.requestms.workflow.enums.WorkflowStatus;
import com.yigit.requestms.workflow.exception.InvalidWorkflowTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "YIGIT_WORKFLOWS")
public class WorkflowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "yigitSeqWorkflows")
    @SequenceGenerator(name = "yigitSeqWorkflows",
            sequenceName = "YIGIT_SEQ_WORKFLOWS", allocationSize = 1)
    @Column(name = "TASK_ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "REQUEST_ID", nullable = false, unique = true)
    private RequestEntity request;

    // Null until someone takes the task, which is what makes an unclaimed
    // backlog possible.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DEVELOPER_ID")
    private UserEntity developer;

    @Enumerated(EnumType.STRING)
    @Column(name = "WORKFLOW_STATUS", nullable = false, length = 30)
    private WorkflowStatus status = WorkflowStatus.BACKLOG;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "ASSIGNED_AT")
    private LocalDateTime assignedAt;

    protected WorkflowEntity() {
    }

    public WorkflowEntity(RequestEntity request) {
        this.request = request;
        this.status = WorkflowStatus.BACKLOG;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public RequestEntity getRequest() {
        return request;
    }

    public UserEntity getDeveloper() {
        return developer;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    // Assignment sets both fields together, which the schema also enforces:
    // an owner without a timestamp says nothing about how long the task waited.
    public void assignTo(UserEntity developer) {
        this.developer = developer;
        this.assignedAt = LocalDateTime.now();
    }

    // No setStatus by design: every move goes through the state machine, so an
    // illegal transition cannot be written by accident.
    public void transitionTo(WorkflowStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidWorkflowTransitionException(status, target);
        }
        this.status = target;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WorkflowEntity that)) {
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
        return "WorkflowEntity{id=" + id + ", status=" + status + "}";
    }
}