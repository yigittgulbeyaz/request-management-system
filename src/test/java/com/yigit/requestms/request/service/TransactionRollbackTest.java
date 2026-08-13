package com.yigit.requestms.request.service;

import com.yigit.requestms.common.security.CurrentUserService;
import com.yigit.requestms.prioritization.dto.PrioritizationFormDto;
import com.yigit.requestms.prioritization.enums.ImpactLevel;
import com.yigit.requestms.prioritization.enums.UrgencyLevel;
import com.yigit.requestms.prioritization.repository.PrioritizationRepository;
import com.yigit.requestms.prioritization.service.PrioritizationService;
import com.yigit.requestms.request.entity.RequestEntity;
import com.yigit.requestms.request.enums.RequestStatus;
import com.yigit.requestms.request.repository.RequestRepository;
import com.yigit.requestms.user.entity.UserEntity;
import com.yigit.requestms.user.repository.UserRepository;
import com.yigit.requestms.workflow.repository.WorkflowRepository;
import com.yigit.requestms.workflow.service.WorkflowService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

// The audit writer is replaced by a mock that fails, which is the only way to
// interrupt a transaction after some of its writes have happened. What matters
// is what the database keeps afterwards.
//
// Not @Transactional, for the same reason as the boundary test: a shared
// transaction never commits, so a rollback could not be told from a success.
@SpringBootTest
@ActiveProfiles("test")
class TransactionRollbackTest {

    @Autowired
    private PrioritizationService prioritizationService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private PrioritizationRepository prioritizationRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private RequestAuditService auditService;

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

    @AfterEach
    void removeWhatTheTestWrote() {
        for (Long requestId : createdRequests) {
            workflowRepository.findAll().stream()
                    .filter(w -> requestId.equals(w.getRequest().getId()))
                    .forEach(workflowRepository::delete);

            prioritizationRepository.findByRequestId(requestId)
                    .ifPresent(prioritizationRepository::delete);

            requestRepository.deleteById(requestId);
        }
        createdRequests.clear();
    }

    @Test
    @DisplayName("a failure after the score leaves neither the score nor the status behind")
    void scoringRollsBackAsAWhole() {
        RequestEntity request = newRequest();

        // The trail entry is written after the score and the status move, so
        // failing here is failing with work already done.
        doThrow(new IllegalStateException("audit unavailable"))
                .when(auditService).recordTransition(any(), any(), any(), any());

        assertThatThrownBy(() -> prioritizationService.score(request.getId(),
                new PrioritizationFormDto(ImpactLevel.MODERATE, UrgencyLevel.SHORT_TERM)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(prioritizationRepository.findByRequestId(request.getId()))
                .as("a score that outlived its own transaction would be a score nobody gave")
                .isEmpty();

        assertThat(requestRepository.findById(request.getId()).orElseThrow().getStatus())
                .as("the status must not move without the score that justified it")
                .isEqualTo(RequestStatus.NEW);
    }

    @Test
    @DisplayName("a failure during conversion leaves no task and no moved request")
    void conversionRollsBackAsAWhole() {
        RequestEntity request = scoredRequest();
        long workflowsBefore = workflowRepository.count();

        doThrow(new IllegalStateException("audit unavailable"))
                .when(auditService).recordTransition(any(), any(), any(), any());

        assertThatThrownBy(() -> workflowService.convertToWorkflow(request.getId()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(workflowRepository.count())
                .as("a task whose request never moved would be work nobody scheduled")
                .isEqualTo(workflowsBefore);

        assertThat(requestRepository.findById(request.getId()).orElseThrow().getStatus())
                .isEqualTo(RequestStatus.PRIORITIZED);
    }

    private RequestEntity newRequest() {
        RequestEntity request = requestRepository.save(new RequestEntity(customer,
                "Rollback fixture",
                "Written by a test and removed by it, never seen by a user."));
        createdRequests.add(request.getId());
        return request;
    }

    // Scored while the audit mock is still silent, so the arrangement itself
    // does not trip the failure the test is about to arrange.
    private RequestEntity scoredRequest() {
        RequestEntity request = newRequest();
        prioritizationService.score(request.getId(),
                new PrioritizationFormDto(ImpactLevel.MODERATE, UrgencyLevel.SHORT_TERM));
        return request;
    }
}