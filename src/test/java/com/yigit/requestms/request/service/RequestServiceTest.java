package com.yigit.requestms.request.service;

import com.yigit.requestms.common.security.CurrentUserService;
import com.yigit.requestms.request.dto.CustomerRequestDetailDto;
import com.yigit.requestms.request.dto.RequestCreateDto;
import com.yigit.requestms.request.entity.RequestEntity;
import com.yigit.requestms.request.enums.RequestStatus;
import com.yigit.requestms.request.exception.RequestNotFoundException;
import com.yigit.requestms.request.mapper.RequestMapper;
import com.yigit.requestms.request.repository.RequestRepository;
import com.yigit.requestms.user.entity.UserEntity;
import com.yigit.requestms.user.enums.Role;
import com.yigit.requestms.user.enums.SecurityQuestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    private static final Long CUSTOMER_ID = 7L;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private RequestMapper requestMapper;

    @InjectMocks
    private RequestService requestService;

    private UserEntity customer;

    @BeforeEach
    void setUp() {
        customer = new UserEntity("Ahmet Yilmaz", "ahmet@example.com", "hash",
                Role.CUSTOMER, SecurityQuestion.BIRTH_CITY, "answerHash");
    }

    @Test
    @DisplayName("submits a request owned by the session user, in NEW status")
    void submitAssignsSessionUserAndInitialStatus() {
        when(currentUserService.require()).thenReturn(customer);
        when(requestRepository.save(any(RequestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        requestService.submit(new RequestCreateDto("Payment screen error",
                "The card form rejects every attempt since this morning."));

        ArgumentCaptor<RequestEntity> saved = ArgumentCaptor.forClass(RequestEntity.class);
        verify(requestRepository).save(saved.capture());

        // The customer comes from the session, never from the DTO: a client
        // cannot raise a request on someone else's behalf.
        assertThat(saved.getValue().getCustomer()).isSameAs(customer);
        assertThat(saved.getValue().getStatus()).isEqualTo(RequestStatus.NEW);
        assertThat(saved.getValue().getCreatedAt()).isNotNull();
        assertThat(saved.getValue().getClosedAt()).isNull();
    }

    @Test
    @DisplayName("lists only the session user's requests")
    void listScopesToSessionUser() {
        when(currentUserService.requireId()).thenReturn(CUSTOMER_ID);
        when(requestRepository.findSummariesByCustomer(eq(CUSTOMER_ID), any()))
                .thenReturn(List.of());

        requestService.listMyRequests(Pageable.ofSize(20));

        verify(requestRepository).findSummariesByCustomer(eq(CUSTOMER_ID), any());
    }

    @Test
    @DisplayName("returns the detail of a request the session user owns")
    void detailReturnsOwnedRequest() {
        RequestEntity entity = new RequestEntity(customer, "Invoice logo missing",
                "The company logo is absent from generated invoice PDFs.");
        CustomerRequestDetailDto expected = new CustomerRequestDetailDto(
                1L, entity.getTitle(), entity.getDescription(),
                RequestStatus.NEW, null, entity.getCreatedAt(), null);

        when(currentUserService.requireId()).thenReturn(CUSTOMER_ID);
        when(requestRepository.findByIdAndCustomerId(1L, CUSTOMER_ID))
                .thenReturn(Optional.of(entity));
        when(requestMapper.toCustomerDetail(entity)).thenReturn(expected);

        assertThat(requestService.getMyRequest(1L)).isEqualTo(expected);
    }

    @Test
    @DisplayName("hides another customer's request behind the not-found failure")
    void detailOfForeignRequestFailsAsNotFound() {
        when(currentUserService.requireId()).thenReturn(CUSTOMER_ID);
        when(requestRepository.findByIdAndCustomerId(99L, CUSTOMER_ID))
                .thenReturn(Optional.empty());

        // A request that exists but belongs to someone else fails exactly like
        // one that does not exist, so ids cannot be probed for real requests.
        assertThatThrownBy(() -> requestService.getMyRequest(99L))
                .isInstanceOf(RequestNotFoundException.class);

        verify(requestMapper, never()).toCustomerDetail(any());
    }
}