package com.yigit.requestms.prioritization.service;

import com.yigit.requestms.common.security.CurrentUserService;
import com.yigit.requestms.prioritization.dto.PrioritizationDetailDto;
import com.yigit.requestms.prioritization.dto.PrioritizationFormDto;
import com.yigit.requestms.prioritization.entity.PrioritizationEntity;
import com.yigit.requestms.prioritization.enums.ImpactLevel;
import com.yigit.requestms.prioritization.enums.UrgencyLevel;
import com.yigit.requestms.prioritization.exception.PrioritizationNotEditableException;
import com.yigit.requestms.prioritization.repository.PrioritizationRepository;
import com.yigit.requestms.request.entity.RequestEntity;
import com.yigit.requestms.request.enums.RequestStatus;
import com.yigit.requestms.request.exception.RequestNotFoundException;
import com.yigit.requestms.request.repository.RequestRepository;
import com.yigit.requestms.request.service.RequestAuditService;
import com.yigit.requestms.user.entity.UserEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('PRODUCT_OWNER')")
public class PrioritizationService {

    private final RequestRepository requestRepository;
    private final PrioritizationRepository prioritizationRepository;
    private final CurrentUserService currentUserService;
    private final RequestAuditService auditService;

    public PrioritizationService(RequestRepository requestRepository,
                                 PrioritizationRepository prioritizationRepository,
                                 CurrentUserService currentUserService,
                                 RequestAuditService auditService) {
        this.requestRepository = requestRepository;
        this.prioritizationRepository = prioritizationRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    // Asked before opening the form rather than discovered on save: navigation
    // needs a yes or no, and an exception is not an answer it can act on.
    @Transactional(readOnly = true)
    public boolean isScorable(Long requestId) {
        return requestRepository.findById(requestId)
                .map(request -> isScorable(request.getStatus()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public PrioritizationDetailDto loadForScoring(Long requestId) {
        RequestEntity request = requireRequest(requestId);
        requireScorable(request);

        return prioritizationRepository.findByRequestId(requestId)
                .map(existing -> detail(request, existing))
                .orElseGet(() -> detail(request, null));
    }

    // Four writes, one transaction: the score, the status, the trail entry and
    // the customer's notice. A score without the status leaves the request
    // invisible to the owner who just entered it; a notice without the score
    // tells the customer about something that did not happen.
    @Transactional
    public void score(Long requestId, PrioritizationFormDto form) {
        RequestEntity request = requireRequest(requestId);
        requireScorable(request);

        UserEntity owner = currentUserService.require();
        RequestStatus previous = request.getStatus();

        prioritizationRepository.findByRequestId(requestId).ifPresentOrElse(
                existing -> existing.revise(form.impact().getValue(), form.urgency().getValue()),
                () -> prioritizationRepository.save(new PrioritizationEntity(
                        request, form.impact().getValue(), form.urgency().getValue(), owner)));

        // Already PRIORITIZED when a score is revised, and the state machine
        // has no self-transition, so the move is only made on first scoring.
        // A revision is a correction to a decision already recorded, not a new
        // event, which is why it writes no trail entry and sends no notice.
        if (previous == RequestStatus.NEW) {
            request.transitionTo(RequestStatus.PRIORITIZED);
            auditService.recordTransition(request, previous, RequestStatus.PRIORITIZED, owner);
            auditService.notify(request.getCustomer(),
                    "Your request has been reviewed and prioritised.", request);
        }
    }

    private RequestEntity requireRequest(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));
    }

    // Checked again on the way in and on the way out. The view asks first so it
    // can redirect, but a caller reaching the service directly must not get
    // past it either.
    private void requireScorable(RequestEntity request) {
        if (!isScorable(request.getStatus())) {
            throw new PrioritizationNotEditableException(request.getStatus());
        }
    }

    // Scoring stops once development starts: changing the number afterwards
    // would rewrite the reason the work was scheduled, after the fact.
    private boolean isScorable(RequestStatus status) {
        return status == RequestStatus.NEW || status == RequestStatus.PRIORITIZED;
    }

    private PrioritizationDetailDto detail(RequestEntity request, PrioritizationEntity scoring) {
        return new PrioritizationDetailDto(
                request.getId(),
                request.getTitle(),
                request.getCustomer().getNameSurname(),
                request.getDescription(),
                scoring == null ? null : ImpactLevel.ofValue(scoring.getImpact()),
                scoring == null ? null : UrgencyLevel.ofValue(scoring.getUrgency()),
                scoring == null ? null : scoring.getPriorityScore());
    }
}