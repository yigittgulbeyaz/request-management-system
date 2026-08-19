package com.yigit.requestms.request.repository;

import com.yigit.requestms.request.dto.StatusTimelineEntryDto;
import com.yigit.requestms.request.entity.RequestStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RequestStatusHistoryRepository
        extends JpaRepository<RequestStatusHistoryEntity, Long> {

    List<RequestStatusHistoryEntity> findByRequestIdOrderByChangedAtDesc(Long requestId);

    // Only the request's own states. The trail also holds the workflow stages,
    // and a customer being shown that their request reached TESTING would be
    // told something about how the work is going that they cannot act on and
    // were never meant to see.
    //
    // Oldest first, because a timeline is read forwards.
    @Query("""
            SELECT new com.yigit.requestms.request.dto.StatusTimelineEntryDto(
                h.newStatus, h.changedAt)
            FROM RequestStatusHistoryEntity h
            WHERE h.request.id = :requestId
              AND h.newStatus IN ('NEW', 'PRIORITIZED', 'IN_WORKFLOW', 'CLOSED', 'REJECTED')
            ORDER BY h.changedAt ASC
            """)
    List<StatusTimelineEntryDto> findCustomerTimeline(@Param("requestId") Long requestId);
}