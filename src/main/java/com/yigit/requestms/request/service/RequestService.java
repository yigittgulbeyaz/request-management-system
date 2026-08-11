package com.yigit.requestms.request.service;

import com.yigit.requestms.common.security.CurrentUserService;
import com.yigit.requestms.request.dto.CustomerRequestDetailDto;
import com.yigit.requestms.request.dto.CustomerRequestDto;
import com.yigit.requestms.request.dto.RequestCreateDto;
import com.yigit.requestms.request.entity.RequestEntity;
import com.yigit.requestms.request.exception.RequestNotFoundException;
import com.yigit.requestms.request.mapper.RequestMapper;
import com.yigit.requestms.request.repository.RequestRepository;
import com.yigit.requestms.user.entity.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RequestService {

    private final RequestRepository requestRepository;
    private final CurrentUserService currentUserService;
    private final RequestMapper requestMapper;

    public RequestService(RequestRepository requestRepository,
                          CurrentUserService currentUserService,
                          RequestMapper requestMapper) {
        this.requestRepository = requestRepository;
        this.currentUserService = currentUserService;
        this.requestMapper = requestMapper;
    }

    // Returns the id rather than the entity: handing back a managed entity would
    // let the caller modify it outside the transaction that created it.
    @Transactional
    public Long submit(RequestCreateDto dto) {
        UserEntity customer = currentUserService.require();
        RequestEntity request = new RequestEntity(customer, dto.title(), dto.description());
        return requestRepository.save(request).getId();
    }

    @Transactional(readOnly = true)
    public List<CustomerRequestDto> listMyRequests(Pageable pageable) {
        return requestRepository.findSummariesByCustomer(
                currentUserService.requireId(), pageable);
    }

    @Transactional(readOnly = true)
    public long countMyRequests() {
        return requestRepository.countByCustomerId(currentUserService.requireId());
    }

    // A request belonging to someone else fails the same way as one that does
    // not exist: telling the two apart would let a customer probe ids to learn
    // which requests are real.
    @Transactional(readOnly = true)
    public CustomerRequestDetailDto getMyRequest(Long requestId) {
        return requestRepository
                .findByIdAndCustomerId(requestId, currentUserService.requireId())
                .map(requestMapper::toCustomerDetail)
                .orElseThrow(() -> new RequestNotFoundException(requestId));
    }
}