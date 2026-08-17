package com.yigit.requestms.request.service;

import com.yigit.requestms.common.security.CurrentUserService;
import com.yigit.requestms.request.dto.RequestSummaryDto;
import com.yigit.requestms.request.entity.RequestEntity;
import com.yigit.requestms.request.enums.RequestStatus;
import com.yigit.requestms.request.exception.RejectionReasonRequiredException;
import com.yigit.requestms.request.exception.RequestNotFoundException;
import com.yigit.requestms.request.repository.RequestRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Every method here is the product owner's work. Declared on the class rather
// than repeated per method: a method added later inherits the restriction
// instead of starting unprotected.
@Service
@PreAuthorize("hasRole('PRODUCT_OWNER')")
public class PoRequestService {

    private static final String SCORE_PROPERTY = "p.priorityScore";

    // Highest score first, and unscored requests last rather than treated as
    // zero: no score means nobody has judged the request yet, which is work to
    // do and not the lowest priority.
    private static final Sort DEFAULT_ORDER = Sort.by(
            Sort.Order.desc(SCORE_PROPERTY).nullsLast(),
            Sort.Order.desc("r.createdAt"));

    private final RequestRepository requestRepository;
    private final CurrentUserService currentUserService;
    private final RequestAuditService auditService;

    public PoRequestService(RequestRepository requestRepository,
                            CurrentUserService currentUserService,
                            RequestAuditService auditService) {
        this.requestRepository = requestRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<RequestSummaryDto> listPool(RequestStatus status, Pageable pageable) {
        return requestRepository.findPoolSummaries(status, resolveOrder(pageable));
    }

    @Transactional(readOnly = true)
    public long countPool(RequestStatus status) {
        return requestRepository.countPoolSummaries(status);
    }

    // Rejection belongs here rather than with scoring: it is a decision about
    // the request itself and can be reached without the request ever having
    // been scored.
    //
    // The reason travels into the notification as well as the record. A dead
    // end with no explanation is worse than no answer at all.
    @Transactional
    public void reject(Long requestId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new RejectionReasonRequiredException();
        }

        RequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));

        RequestStatus previous = request.getStatus();
        request.markRejected(reason.trim());

        auditService.recordTransition(request, previous, RequestStatus.REJECTED,
                currentUserService.require());
        auditService.notify(request.getCustomer(),
                "Your request was not taken forward.", request);
    }

    // Applied here rather than in the query so that a sort chosen in the grid
    // replaces it outright instead of being appended behind it.
    private Pageable resolveOrder(Pageable pageable) {
        Sort sort = pageable.getSort().isSorted()
                ? keepUnscoredLast(pageable.getSort())
                : DEFAULT_ORDER;

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    // Unscored requests belong at the bottom whichever way the score column is
    // sorted. Descending would otherwise open with the rows that have no score
    // at all, which is the opposite of what "highest first" is asking for.
    private Sort keepUnscoredLast(Sort sort) {
        return Sort.by(sort.stream()
                .map(order -> SCORE_PROPERTY.equals(order.getProperty())
                        ? order.nullsLast()
                        : order)
                .toList());
    }
}