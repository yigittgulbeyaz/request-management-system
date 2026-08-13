package com.yigit.requestms.request.service;

import com.yigit.requestms.notification.entity.NotificationEntity;
import com.yigit.requestms.notification.repository.NotificationRepository;
import com.yigit.requestms.request.entity.RequestEntity;
import com.yigit.requestms.request.entity.RequestStatusHistoryEntity;
import com.yigit.requestms.request.repository.RequestStatusHistoryRepository;
import com.yigit.requestms.user.entity.UserEntity;
import org.springframework.stereotype.Service;

// Writes the two records that accompany every state change. Kept in one place
// because the alternative is the same two lines repeated in four services,
// where one of them eventually gets forgotten.
//
// No @Transactional of its own: these calls join the transaction of the change
// they describe. A trail entry that survives a rolled-back change would be a
// record of something that never happened.
@Service
public class RequestAuditService {

    private final RequestStatusHistoryRepository historyRepository;
    private final NotificationRepository notificationRepository;

    public RequestAuditService(RequestStatusHistoryRepository historyRepository,
                               NotificationRepository notificationRepository) {
        this.historyRepository = historyRepository;
        this.notificationRepository = notificationRepository;
    }

    public void recordTransition(RequestEntity request, Enum<?> from, Enum<?> to,
                                 UserEntity actor) {
        historyRepository.save(new RequestStatusHistoryEntity(request, from, to, actor));
    }

    public void notify(UserEntity recipient, String message, RequestEntity relatedRequest) {
        notificationRepository.save(
                new NotificationEntity(recipient, message, relatedRequest));
    }
}