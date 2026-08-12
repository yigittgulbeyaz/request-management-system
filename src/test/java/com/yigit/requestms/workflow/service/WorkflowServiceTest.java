package com.yigit.requestms.workflow.service;

import com.yigit.requestms.request.entity.RequestEntity;
import com.yigit.requestms.request.enums.RequestStatus;
import com.yigit.requestms.request.exception.InvalidRequestTransitionException;
import com.yigit.requestms.request.exception.RequestNotFoundException;
import com.yigit.requestms.request.repository.RequestRepository;
import com.yigit.requestms.user.entity.UserEntity;
import com.yigit.requestms.user.enums.Role;
import com.yigit.requestms.user.enums.SecurityQuestion;
import com.yigit.requestms.workflow.entity.WorkflowEntity;
import com.yigit.requestms.workflow.enums.WorkflowStatus;
import com.yigit.requestms.workflow.exception.WorkflowAlreadyExistsException;
import com.yigit.requestms.workflow.repository.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    private static final Long REQUEST_ID = 42L;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private WorkflowRepository workflowRepository;

    @InjectMocks
    private WorkflowService workflowService;

    private UserEntity customer;

    @BeforeEach
    void setUp() {
        customer = new UserEntity("Ahmet Yilmaz", "ahmet@example.com", "hash",
                Role.CUSTOMER, SecurityQuestion.BIRTH_CITY, "answerHash");
    }

    @Test
    @DisplayName("opens the task in BACKLOG with nobody assigned")
    void conversionLeavesTheTaskUnclaimed() {
        RequestEntity request = prioritizedRequest();

        when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(workflowRepository.existsByRequestId(REQUEST_ID)).thenReturn(false);
        when(workflowRepository.save(any(WorkflowEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        workflowService.convertToWorkflow(REQUEST_ID);

        ArgumentCaptor<WorkflowEntity> saved = ArgumentCaptor.forClass(WorkflowEntity.class);
        verify(workflowRepository).save(saved.capture());

        // Unclaimed is the point: the task waits to be pulled rather than being
        // handed to whoever the owner happened to pick at conversion.
        assertThat(saved.getValue().getStatus()).isEqualTo(WorkflowStatus.BACKLOG);
        assertThat(saved.getValue().getDeveloper()).isNull();
        assertThat(saved.getValue().getAssignedAt()).isNull();
    }

    @Test
    @DisplayName("moves the request to IN_WORKFLOW alongside creating the task")
    void conversionMovesTheRequest() {
        RequestEntity request = prioritizedRequest();

        when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(workflowRepository.existsByRequestId(REQUEST_ID)).thenReturn(false);
        when(workflowRepository.save(any(WorkflowEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        workflowService.convertToWorkflow(REQUEST_ID);

        assertThat(request.getStatus()).isEqualTo(RequestStatus.IN_WORKFLOW);
    }

    @Test
    @DisplayName("refuses a request that was never scored")
    void unscoredRequestCannotBeConverted() {
        RequestEntity request = new RequestEntity(customer, "Login API failure",
                "Users are timing out during peak hours, several times a day.");

        when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(workflowRepository.existsByRequestId(REQUEST_ID)).thenReturn(false);

        // Scheduling work nobody has judged would put it in the queue with no
        // answer to the question of where in the queue it belongs.
        assertThatThrownBy(() -> workflowService.convertToWorkflow(REQUEST_ID))
                .isInstanceOf(InvalidRequestTransitionException.class);

        verify(workflowRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuses a second conversion by name, not by constraint violation")
    void secondConversionIsRefused() {
        when(requestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(prioritizedRequest()));
        when(workflowRepository.existsByRequestId(REQUEST_ID)).thenReturn(true);

        assertThatThrownBy(() -> workflowService.convertToWorkflow(REQUEST_ID))
                .isInstanceOf(WorkflowAlreadyExistsException.class);

        verify(workflowRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuses a request that does not exist")
    void missingRequestIsRefused() {
        when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workflowService.convertToWorkflow(REQUEST_ID))
                .isInstanceOf(RequestNotFoundException.class);

        verify(workflowRepository, never()).save(any());
    }

    private RequestEntity prioritizedRequest() {
        RequestEntity request = new RequestEntity(customer, "Excel export is empty",
                "The monthly sales report downloads as a zero byte file.");
        request.transitionTo(RequestStatus.PRIORITIZED);
        return request;
    }
}