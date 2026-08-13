package com.yigit.requestms.request.service;

import com.yigit.requestms.common.security.CurrentUserService;
import com.yigit.requestms.notification.repository.NotificationRepository;
import com.yigit.requestms.prioritization.dto.PrioritizationFormDto;
import com.yigit.requestms.prioritization.enums.ImpactLevel;
import com.yigit.requestms.prioritization.enums.UrgencyLevel;
import com.yigit.requestms.prioritization.repository.PrioritizationRepository;
import com.yigit.requestms.prioritization.service.PrioritizationService;
import com.yigit.requestms.request.entity.RequestEntity;
import com.yigit.requestms.request.enums.RequestStatus;
import com.yigit.requestms.request.repository.RequestRepository;
import com.yigit.requestms.request.repository.RequestStatusHistoryRepository;
import com.yigit.requestms.user.entity.UserEntity;
import com.yigit.requestms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Deliberately not @Transactional. A test that wraps the service in its own
// transaction shares it, so nothing ever commits and nothing ever rolls back:
// the very thing under test would be invisible. The rows are written for real
// and removed afterwards instead.
@SpringBootTest
@ActiveProfiles("test")
class TransactionBoundaryTest {

    @Autowired
    private PrioritizationService prioritizationService;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private PrioritizationRepository prioritizationRepository;

    @Autowired
    private RequestStatusHistoryRepository historyRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    // The acting user normally comes from the session, which a test has none of.
    @MockitoBean
    private CurrentUserService currentUserService;

    private UserEntity customer;
    private UserEntity productOwner;
    private final List<Long> createdRequests = new ArrayList<>();

    @BeforeEach
    void setUp() {
        customer = userRepository.findByEmail("ahmet.yilmaz@teknocorp.com").orElseThrow();
        productOwner = userRepository.findByEmail("elif.kaya@company.com").orElseThrow();

        when(currentUserService.require()).thenReturn(productOwner);
        when(currentUserService.requireId()).thenReturn(productOwner.getId());
    }

    // Children first, then the request itself: the foreign keys have no cascade
    // because nothing in this system is ever deleted in normal use.
    @AfterEach
    void removeWhatTheTestWrote() {
        for (Long requestId : createdRequests) {
            notificationRepository.deleteAll(
                    notificationRepository.findByRecipientIdOrderByCreatedAtDesc(customer.getId())
                            .stream()
                            .filter(n -> n.getRelatedRequest() != null
                                    && requestId.equals(n.getRelatedRequest().getId()))
                            .toList());

            historyRepository.deleteAll(
                    historyRepository.findByRequestIdOrderByChangedAtDesc(requestId));

            prioritizationRepository.findByRequestId(requestId)
                    .ifPresent(prioritizationRepository::delete);

            requestRepository.deleteById(requestId);
        }
        createdRequests.clear();
    }

    @Test
    @DisplayName("scoring commits the score, the status, the trail and the notice together")
    void scoringWritesEverything() {
        RequestEntity request = newRequest();
        long historyBefore = historyRepository.count();
        long noticesBefore = notificationRepository.count();

        prioritizationService.score(request.getId(),
                new PrioritizationFormDto(ImpactLevel.MODERATE, UrgencyLevel.SHORT_TERM));

        // Read back rather than trusting the in-memory entity: the question is
        // what the database holds now that the transaction has committed.
        RequestEntity stored = requestRepository.findById(request.getId()).orElseThrow();

        assertThat(stored.getStatus()).isEqualTo(RequestStatus.PRIORITIZED);
        assertThat(prioritizationRepository.findByRequestId(request.getId())).isPresent();
        assertThat(historyRepository.count()).isEqualTo(historyBefore + 1);
        assertThat(notificationRepository.count()).isEqualTo(noticesBefore + 1);
    }

    @Test
    @DisplayName("the score the database stores is the one it derived, not one Java sent")
    void scoreIsDerivedByTheDatabase() {
        RequestEntity request = newRequest();

        prioritizationService.score(request.getId(),
                new PrioritizationFormDto(ImpactLevel.MAJOR, UrgencyLevel.IMMEDIATE));

        // Java never sends a score. Four times five is the database's answer,
        // read back here for the first time.
        assertThat(prioritizationRepository.findByRequestId(request.getId()))
                .get()
                .extracting("priorityScore")
                .isEqualTo(20);
    }

    @Test
    @DisplayName("revising a score writes no second trail entry and no second notice")
    void revisionIsNotANewEvent() {
        RequestEntity request = newRequest();

        prioritizationService.score(request.getId(),
                new PrioritizationFormDto(ImpactLevel.MODERATE, UrgencyLevel.SHORT_TERM));

        long historyAfterFirst = historyRepository.count();
        long noticesAfterFirst = notificationRepository.count();

        prioritizationService.score(request.getId(),
                new PrioritizationFormDto(ImpactLevel.CRITICAL, UrgencyLevel.IMMEDIATE));

        // A correction to a decision already recorded is not a new decision.
        assertThat(historyRepository.count()).isEqualTo(historyAfterFirst);
        assertThat(notificationRepository.count()).isEqualTo(noticesAfterFirst);

        assertThat(prioritizationRepository.findByRequestId(request.getId()))
                .get()
                .extracting("priorityScore")
                .isEqualTo(25);
    }

    private RequestEntity newRequest() {
        RequestEntity request = requestRepository.save(new RequestEntity(customer,
                "Transaction boundary fixture",
                "Written by a test and removed by it, never seen by a user."));
        createdRequests.add(request.getId());
        return request;
    }
}