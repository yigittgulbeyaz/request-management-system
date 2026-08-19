package com.yigit.requestms.notification.service;

import com.yigit.requestms.common.security.CurrentUserService;
import com.yigit.requestms.notification.dto.NotificationDto;
import com.yigit.requestms.notification.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Everything here reads or writes the caller's own notices, resolved from the
// session. No role restriction: every role is notified about something.
@Service
@PreAuthorize("isAuthenticated()")
public class NotificationService {

    // Enough to see what happened while you were away without turning a menu
    // into a screen. Anything older is in the request itself.
    private static final int MENU_SIZE = 15;

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    public NotificationService(NotificationRepository notificationRepository,
                               CurrentUserService currentUserService) {
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> recent() {
        return notificationRepository.findRecentFor(
                currentUserService.requireId(), PageRequest.ofSize(MENU_SIZE));
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByRecipientIdAndReadFalse(
                currentUserService.requireId());
    }

    // Marking read is deliberate rather than automatic on opening the menu.
    // Opening it to check something is not the same as having dealt with what
    // is in it, and a badge that clears itself on a glance stops meaning
    // anything.
    @Transactional
    public void markAllRead() {
        notificationRepository.markAllRead(currentUserService.requireId());
    }
}