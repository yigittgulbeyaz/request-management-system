package com.yigit.requestms.notification.repository;

import com.yigit.requestms.notification.dto.NotificationDto;
import com.yigit.requestms.notification.entity.NotificationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    // A projection rather than the entities: the menu shows a handful of lines
    // and has no use for the recipient or the request behind each one.
    //
    // Newest first, because a notice list is read backwards — the opposite of a
    // timeline, which tells a story.
    @Query("""
            SELECT new com.yigit.requestms.notification.dto.NotificationDto(
                n.id, n.message, n.read, r.id, n.createdAt)
            FROM NotificationEntity n
            LEFT JOIN n.relatedRequest r
            WHERE n.recipient.id = :recipientId
            ORDER BY n.createdAt DESC
            """)
    List<NotificationDto> findRecentFor(@Param("recipientId") Long recipientId,
                                        Pageable pageable);

    // Used when a test removes what it wrote. In normal use nothing deletes a
    // notification, which is why this is the only thing that asks for them by
    // request.
    List<NotificationEntity> findByRelatedRequestId(Long requestId);

    long countByRecipientIdAndReadFalse(Long recipientId);

    // One statement rather than loading every unread row to flip a flag on it.
    // Marking all as read is a bulk act and reads like one here.
    @Modifying
    @Query("""
            UPDATE NotificationEntity n
            SET n.read = true
            WHERE n.recipient.id = :recipientId AND n.read = false
            """)
    int markAllRead(@Param("recipientId") Long recipientId);
}