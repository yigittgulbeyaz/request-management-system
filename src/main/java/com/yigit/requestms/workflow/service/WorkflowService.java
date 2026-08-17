package com.yigit.requestms.workflow.service;

import com.yigit.requestms.common.security.CurrentUserService;
import com.yigit.requestms.request.entity.RequestEntity;
import com.yigit.requestms.request.enums.RequestStatus;
import com.yigit.requestms.request.exception.RequestNotFoundException;
import com.yigit.requestms.request.repository.RequestRepository;
import com.yigit.requestms.request.service.RequestAuditService;
import com.yigit.requestms.user.entity.UserEntity;
import com.yigit.requestms.workflow.dto.TaskSummaryDto;
import com.yigit.requestms.workflow.entity.WorkflowEntity;
import com.yigit.requestms.workflow.enums.WorkflowStatus;
import com.yigit.requestms.workflow.exception.TaskAlreadyClaimedException;
import com.yigit.requestms.workflow.exception.TaskNotAssignedToYouException;
import com.yigit.requestms.workflow.exception.TaskNotFoundException;
import com.yigit.requestms.workflow.exception.WorkflowAlreadyExistsException;
import com.yigit.requestms.workflow.repository.WorkflowRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class WorkflowService {

    private final RequestRepository requestRepository;
    private final WorkflowRepository workflowRepository;
    private final CurrentUserService currentUserService;
    private final RequestAuditService auditService;

    public WorkflowService(RequestRepository requestRepository,
                           WorkflowRepository workflowRepository,
                           CurrentUserService currentUserService,
                           RequestAuditService auditService) {
        this.requestRepository = requestRepository;
        this.workflowRepository = workflowRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    // Creating the task and moving the request happen together. A task whose
    // request still reads PRIORITIZED would show up twice: once as work in
    // progress, once as work still waiting to be scheduled.
    //
    // No developer is assigned here. The task waits in the backlog until
    // someone takes it, which is what lets developers pull work rather than
    // only receive it.
    @Transactional
    public Long convertToWorkflow(Long requestId) {
        RequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));

        // The unique constraint would catch this too, but a rule the user
        // broke deserves a sentence rather than a constraint violation.
        if (workflowRepository.existsByRequestId(requestId)) {
            throw new WorkflowAlreadyExistsException(requestId);
        }

        RequestStatus previous = request.getStatus();
        request.transitionTo(RequestStatus.IN_WORKFLOW);

        auditService.recordTransition(request, previous, RequestStatus.IN_WORKFLOW,
                currentUserService.require());

        return workflowRepository.save(new WorkflowEntity(request)).getId();
    }

    @Transactional(readOnly = true)
    public List<TaskSummaryDto> listMyTasks(WorkflowStatus status, Pageable pageable) {
        return workflowRepository.findAssignedTo(
                currentUserService.requireId(), status, pageable);
    }

    @Transactional(readOnly = true)
    public long countMyTasks(WorkflowStatus status) {
        return workflowRepository.countAssignedTo(currentUserService.requireId(), status);
    }

    @Transactional(readOnly = true)
    public List<TaskSummaryDto> listUnclaimed(Pageable pageable) {
        return workflowRepository.findUnclaimed(pageable);
    }

    @Transactional(readOnly = true)
    public long countUnclaimed() {
        return workflowRepository.countUnclaimed();
    }

    // The check and the write have to see the same state, so the row is locked
    // for the whole of it. Without the lock two developers both read an
    // unclaimed task, both pass the check, and the second write silently
    // replaces the first: the loser is never told they lost.
    @Transactional
    public void claim(Long taskId) {
        WorkflowEntity task = workflowRepository.findByIdForUpdate(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        if (task.getDeveloper() != null) {
            throw new TaskAlreadyClaimedException(taskId);
        }

        task.assignTo(currentUserService.require());
    }

    // Everything the move implies happens together: the stage, the trail entry,
    // and on DONE the closing of the request and the notice to the customer.
    //
    // The request closes on its own: a developer saying the work is finished is
    // the whole of the decision, because there is no reviewer role here to put
    // an approval step behind.
    @Transactional
    public void advance(Long taskId, WorkflowStatus target) {
        WorkflowEntity task = requireTask(taskId);
        requireOwnership(task);

        UserEntity actor = currentUserService.require();
        WorkflowStatus previousStage = task.getStatus();
        RequestEntity request = task.getRequest();

        task.transitionTo(target);
        auditService.recordTransition(request, previousStage, target, actor);

        if (target == WorkflowStatus.DONE) {
            RequestStatus previousStatus = request.getStatus();
            request.markClosed(LocalDateTime.now());

            auditService.recordTransition(request, previousStatus, RequestStatus.CLOSED, actor);
            auditService.notify(request.getCustomer(),
                    "Your request has been completed.", request);
        }
    }

    private WorkflowEntity requireTask(Long taskId) {
        return workflowRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    // Compares ids rather than entities. The developer association is lazy, so
    // the getter hands back a proxy whose own field is not populated; equals on
    // it reads a null id and refuses a task the caller does own. Reading the id
    // forces the proxy to load first.
    //
    // Objects.equals rather than a direct call: an id is null on an entity that
    // has not been persisted, and dereferencing it would fail before the
    // comparison ever ran.
    private void requireOwnership(WorkflowEntity task) {
        UserEntity assignee = task.getDeveloper();
        if (assignee == null
                || !Objects.equals(assignee.getId(), currentUserService.requireId())) {
            throw new TaskNotAssignedToYouException(task.getId());
        }
    }
}