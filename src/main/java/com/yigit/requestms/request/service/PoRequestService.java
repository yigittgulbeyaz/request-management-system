package com.yigit.requestms.request.service;

import com.yigit.requestms.request.dto.RequestSummaryDto;
import com.yigit.requestms.request.enums.RequestStatus;
import com.yigit.requestms.request.repository.RequestRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Kept apart from RequestService: that one answers "my requests" and resolves
// the customer from the session, this one reads across every customer. Merging
// them would put two different authorisation rules behind one class.
@Service
public class PoRequestService {

    private static final String SCORE_PROPERTY = "p.priorityScore";

    // Highest score first, and unscored requests last rather than treated as
    // zero: no score means nobody has judged the request yet, which is work to
    // do and not the lowest priority.
    private static final Sort DEFAULT_ORDER = Sort.by(
            Sort.Order.desc(SCORE_PROPERTY).nullsLast(),
            Sort.Order.desc("r.createdAt"));

    private final RequestRepository requestRepository;

    public PoRequestService(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    @Transactional(readOnly = true)
    public List<RequestSummaryDto> listPool(RequestStatus status, Pageable pageable) {
        return requestRepository.findPoolSummaries(status, resolveOrder(pageable));
    }

    @Transactional(readOnly = true)
    public long countPool(RequestStatus status) {
        return requestRepository.countPoolSummaries(status);
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