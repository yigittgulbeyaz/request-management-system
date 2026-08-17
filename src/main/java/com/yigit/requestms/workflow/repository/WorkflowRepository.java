package com.yigit.requestms.workflow.repository;

import com.yigit.requestms.workflow.dto.TaskSummaryDto;
import com.yigit.requestms.workflow.entity.WorkflowEntity;
import com.yigit.requestms.workflow.enums.WorkflowStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkflowRepository extends JpaRepository<WorkflowEntity, Long> {

    boolean existsByRequestId(Long requestId);

    // Reads the row and holds it until the transaction ends. Two developers
    // claiming at the same moment both reach this line; the second waits here
    // rather than reading a row the first is about to change, and finds it
    // taken when it finally reads.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WorkflowEntity w WHERE w.id = :taskId")
    Optional<WorkflowEntity> findByIdForUpdate(@Param("taskId") Long taskId);

    // Same projection shape as the pool: one query for the page, and the
    // request's CLOB description stays in the database.
    @Query("""
            SELECT new com.yigit.requestms.workflow.dto.TaskSummaryDto(
                w.id, r.id, r.title, p.priorityScore, w.status, d.nameSurname,
                w.createdAt, w.deadline)
            FROM WorkflowEntity w
            JOIN w.request r
            LEFT JOIN PrioritizationEntity p ON p.request = r
            LEFT JOIN w.developer d
            WHERE w.developer.id = :developerId
              AND (:status IS NULL OR w.status = :status)
            """)
    List<TaskSummaryDto> findAssignedTo(@Param("developerId") Long developerId,
                                        @Param("status") WorkflowStatus status,
                                        Pageable pageable);

    @Query("""
            SELECT COUNT(w)
            FROM WorkflowEntity w
            WHERE w.developer.id = :developerId
              AND (:status IS NULL OR w.status = :status)
            """)
    long countAssignedTo(@Param("developerId") Long developerId,
                         @Param("status") WorkflowStatus status);

    // Unclaimed work is only ever in BACKLOG, so the status is fixed rather
    // than a parameter.
    @Query("""
            SELECT new com.yigit.requestms.workflow.dto.TaskSummaryDto(
                w.id, r.id, r.title, p.priorityScore, w.status, NULL,
                w.createdAt, w.deadline)
            FROM WorkflowEntity w
            JOIN w.request r
            LEFT JOIN PrioritizationEntity p ON p.request = r
            WHERE w.developer IS NULL
            """)
    List<TaskSummaryDto> findUnclaimed(Pageable pageable);

    @Query("SELECT COUNT(w) FROM WorkflowEntity w WHERE w.developer IS NULL")
    long countUnclaimed();
}