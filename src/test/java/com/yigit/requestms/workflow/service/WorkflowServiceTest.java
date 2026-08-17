package com.yigit.requestms.workflow.service;

import com.yigit.requestms.common.security.CurrentUserService;
import com.yigit.requestms.prioritization.entity.PrioritizationEntity;
import com.yigit.requestms.prioritization.repository.PrioritizationRepository;
import com.yigit.requestms.request.entity.RequestEntity;
import com.yigit.requestms.request.enums.RequestStatus;
import com.yigit.requestms.request.exception.InvalidRequestTransitionException;
import com.yigit.requestms.request.exception.RequestNotFoundException;
import com.yigit.requestms.request.repository.RequestRepository;
import com.yigit.requestms.request.service.RequestAuditService;
import com.yigit.requestms.user.entity.UserEntity;
import com.yigit.requestms.user.enums.Role;
import com.yigit.requestms.user.enums.SecurityQuestion;
import com.yigit.requestms.workflow.entity.WorkflowEntity;
import com.yigit.requestms.workflow.enums.WorkflowStatus;
import com.yigit.requestms.workflow.exception.InvalidWorkflowTransitionException;
import com.yigit.requestms.workflow.exception.TaskAlreadyClaimedException;
import com.yigit.requestms.workflow.exception.TaskNotAssignedToYouException;
import com.yigit.requestms.workflow.exception.TaskNotFoundException;
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

import java.lang.reflect.Field;
import java.time.LocalDateTime;
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
    private static final Long TASK_ID = 7L;
    private static final Long DEVELOPER_ID = 11L;
    private static final Long OTHER_DEVELOPER_ID = 12L;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private PrioritizationRepository prioritizationRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private RequestAuditService auditService;

    @InjectMocks
    private WorkflowService workflowService;

    private UserEntity customer;
    private UserEntity developer;
    private UserEntity otherDeveloper;

    @BeforeEach
    void setUp() {
        customer = user("Ahmet Yilmaz", "ahmet@example.com", Role.CUSTOMER, 1L);
        developer = user("Deniz Yildirim", "deniz@example.com", Role.DEVELOPER, DEVELOPER_ID);
        otherDeveloper = user("Can Ozturk", "can@example.com", Role.DEVELOPER, OTHER_DEVELOPER_ID);
    }

    // --- conversion -------------------------------------------------------

    @Test
    @DisplayName("opens the task in BACKLOG with nobody assigned")
    void conversionLeavesTheTaskUnclaimed() {
        RequestEntity request = prioritizedRequest();

        when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(workflowRepository.existsByRequestId(REQUEST_ID)).thenReturn(false);
        when(prioritizationRepository.findByRequestId(REQUEST_ID))
                .thenReturn(Optional.of(scoring(request, 3, 4)));
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
        when(prioritizationRepository.findByRequestId(REQUEST_ID))
                .thenReturn(Optional.of(scoring(request, 3, 4)));
        when(workflowRepository.save(any(WorkflowEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        workflowService.convertToWorkflow(REQUEST_ID);

        assertThat(request.getStatus()).isEqualTo(RequestStatus.IN_WORKFLOW);
    }

    @Test
    @DisplayName("a critical request gets days where a low one gets weeks")
    void deadlineFollowsTheScore() {
        RequestEntity critical = prioritizedRequest();

        when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(critical));
        when(workflowRepository.existsByRequestId(REQUEST_ID)).thenReturn(false);
        when(prioritizationRepository.findByRequestId(REQUEST_ID))
                .thenReturn(Optional.of(scoring(critical, 5, 5)));
        when(workflowRepository.save(any(WorkflowEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        workflowService.convertToWorkflow(REQUEST_ID);

        ArgumentCaptor<WorkflowEntity> saved = ArgumentCaptor.forClass(WorkflowEntity.class);
        verify(workflowRepository).save(saved.capture());

        // Twenty-five is critical, which the band gives two days. Asserting the
        // day rather than the exact instant: the promise is a date, not a
        // timestamp anyone will hold to the second.
        assertThat(saved.getValue().getDeadline())
                .isNotNull()
                .isAfter(LocalDateTime.now().plusDays(1))
                .isBefore(LocalDateTime.now().plusDays(3));
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

    // --- claiming ---------------------------------------------------------

    @Test
    @DisplayName("claiming assigns the session user and stamps the time")
    void claimAssignsSessionUser() {
        WorkflowEntity task = unclaimedTask();

        when(workflowRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task));
        when(currentUserService.require()).thenReturn(developer);

        workflowService.claim(TASK_ID);

        assertThat(task.getDeveloper()).isSameAs(developer);
        // Both fields move together, which the schema also enforces: an owner
        // without a timestamp says nothing about how long the task waited.
        assertThat(task.getAssignedAt()).isNotNull();
    }

    @Test
    @DisplayName("claiming does not move the deadline")
    void claimingLeavesTheDeadlineAlone() {
        WorkflowEntity task = unclaimedTask();
        LocalDateTime promised = task.getDeadline();

        when(workflowRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task));
        when(currentUserService.require()).thenReturn(developer);

        workflowService.claim(TASK_ID);

        // A developer inherits the promise, they do not make one by taking the
        // task. Resetting the clock on assignment would let work sit in the
        // backlog for a month and still be on time.
        assertThat(task.getDeadline()).isEqualTo(promised);
    }

    @Test
    @DisplayName("refuses a task someone else already took")
    void claimingATakenTaskIsRefused() {
        WorkflowEntity task = unclaimedTask();
        task.assignTo(otherDeveloper);

        when(workflowRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> workflowService.claim(TASK_ID))
                .isInstanceOf(TaskAlreadyClaimedException.class);

        assertThat(task.getDeveloper()).isSameAs(otherDeveloper);
    }

    @Test
    @DisplayName("refuses a task that does not exist")
    void claimingAMissingTaskIsRefused() {
        when(workflowRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workflowService.claim(TASK_ID))
                .isInstanceOf(TaskNotFoundException.class);
    }

    // --- advancing --------------------------------------------------------

    @Test
    @DisplayName("advances a task the session user owns")
    void advanceMovesTheStage() {
        WorkflowEntity task = claimedTask();

        when(workflowRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(currentUserService.requireId()).thenReturn(DEVELOPER_ID);

        workflowService.advance(TASK_ID, WorkflowStatus.IN_PROGRESS);

        assertThat(task.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("finishing the task closes the customer's request with it")
    void doneClosesTheRequest() {
        WorkflowEntity task = claimedTask();
        task.transitionTo(WorkflowStatus.IN_PROGRESS);
        task.transitionTo(WorkflowStatus.TESTING);

        when(workflowRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(currentUserService.requireId()).thenReturn(DEVELOPER_ID);

        workflowService.advance(TASK_ID, WorkflowStatus.DONE);

        // No owner approval step: a developer saying the work is finished is
        // the whole of the decision, and closedAt is what the resolution time
        // report reads.
        assertThat(task.getRequest().getStatus()).isEqualTo(RequestStatus.CLOSED);
        assertThat(task.getRequest().getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("a finished task stops being overdue")
    void finishedWorkIsNotStillRunningLate() {
        WorkflowEntity task = taskDueAt(LocalDateTime.now().minusDays(3));
        assertThat(task.isOverdue()).isTrue();

        task.transitionTo(WorkflowStatus.IN_PROGRESS);
        task.transitionTo(WorkflowStatus.TESTING);
        task.transitionTo(WorkflowStatus.DONE);

        // Delivered late is late, and the report will say so; it is not still
        // running out of time.
        assertThat(task.isOverdue()).isFalse();
    }

    @Test
    @DisplayName("refuses to advance a task assigned to another developer")
    void advancingSomeoneElsesTaskIsRefused() {
        WorkflowEntity task = unclaimedTask();
        task.assignTo(otherDeveloper);

        when(workflowRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(currentUserService.requireId()).thenReturn(DEVELOPER_ID);

        assertThatThrownBy(() -> workflowService.advance(TASK_ID, WorkflowStatus.IN_PROGRESS))
                .isInstanceOf(TaskNotAssignedToYouException.class);

        assertThat(task.getStatus()).isEqualTo(WorkflowStatus.BACKLOG);
    }

    @Test
    @DisplayName("refuses to advance a task nobody has taken")
    void advancingAnUnclaimedTaskIsRefused() {
        WorkflowEntity task = unclaimedTask();

        when(workflowRepository.findById(TASK_ID)).thenReturn(Optional.of(task));

        // Work cannot start without an owner: the schema says so too, through
        // the check that keeps an unassigned task in BACKLOG. No session lookup
        // is stubbed because the null assignee is refused before one happens.
        assertThatThrownBy(() -> workflowService.advance(TASK_ID, WorkflowStatus.IN_PROGRESS))
                .isInstanceOf(TaskNotAssignedToYouException.class);
    }

    @Test
    @DisplayName("refuses a stage the board does not allow, even for the owner")
    void skippingAStageIsRefused() {
        WorkflowEntity task = claimedTask();

        when(workflowRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(currentUserService.requireId()).thenReturn(DEVELOPER_ID);

        assertThatThrownBy(() -> workflowService.advance(TASK_ID, WorkflowStatus.DONE))
                .isInstanceOf(InvalidWorkflowTransitionException.class);

        // The refusal leaves nothing half-applied: the request is untouched
        // because the stage never moved.
        assertThat(task.getStatus()).isEqualTo(WorkflowStatus.BACKLOG);
        assertThat(task.getRequest().getStatus()).isEqualTo(RequestStatus.IN_WORKFLOW);
    }

    // --- fixtures ---------------------------------------------------------

    private RequestEntity prioritizedRequest() {
        RequestEntity request = new RequestEntity(customer, "Excel export is empty",
                "The monthly sales report downloads as a zero byte file.");
        request.transitionTo(RequestStatus.PRIORITIZED);
        return request;
    }

    private WorkflowEntity unclaimedTask() {
        return taskDueAt(LocalDateTime.now().plusDays(10));
    }

    private WorkflowEntity taskDueAt(LocalDateTime deadline) {
        RequestEntity request = prioritizedRequest();
        request.transitionTo(RequestStatus.IN_WORKFLOW);
        return new WorkflowEntity(request, deadline);
    }

    private WorkflowEntity claimedTask() {
        WorkflowEntity task = unclaimedTask();
        task.assignTo(developer);
        return task;
    }

    // The virtual column is computed by the database, so a scoring built in
    // memory has no score of its own. Reflection puts one there rather than
    // opening a setter the application must never call.
    private PrioritizationEntity scoring(RequestEntity request, int impact, int urgency) {
        PrioritizationEntity entity =
                new PrioritizationEntity(request, impact, urgency, developer);
        assignField(entity, PrioritizationEntity.class, "priorityScore", impact * urgency);
        return entity;
    }

    private UserEntity user(String name, String email, Role role, Long id) {
        UserEntity user = new UserEntity(name, email, "hash", role,
                SecurityQuestion.BIRTH_CITY, "answerHash");
        assignField(user, UserEntity.class, "id", id);
        return user;
    }

    // The entities have no setters for these on purpose: one is assigned by
    // Hibernate, the other computed by the database. A test that never persists
    // still has to supply them, and reflection is the smaller concession.
    private void assignField(Object target, Class<?> type, String name, Object value) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not set " + name, e);
        }
    }
}