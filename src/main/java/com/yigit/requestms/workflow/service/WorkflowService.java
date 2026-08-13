package com.yigit.requestms.workflow.service;

import com.yigit.requestms.common.security.CurrentUserService;
import com.yigit.requestms.request.entity.RequestEntity;
import com.yigit.requestms.request.enums.RequestStatus;
import com.yigit.requestms.request.exception.RequestNotFoundException;
import com.yigit.requestms.request.repository.RequestRepository;
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

@Service
public class WorkflowService {

    private final RequestRepository requestRepository;
    private final WorkflowRepository workflowRepository;
    private final CurrentUserService currentUserService;

    public WorkflowService(RequestRepository requestRepository,
                           WorkflowRepository workflowRepository,
                           CurrentUserService currentUserService) {
        this.requestRepository = requestRepository;
        this.workflowRepository = workflowRepository;
        this.currentUserService = currentUserService;
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

        request.transitionTo(RequestStatus.IN_WORKFLOW);
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

    // Two developers can reach this at the same moment and both see the task as
    // free. Checking again after loading narrows the window but does not close
    // it; row locking does, and arrives with the concurrency work.
    @Transactional
    public void claim(Long taskId) {
        WorkflowEntity task = requireTask(taskId);

        if (task.getDeveloper() != null) {
            throw new TaskAlreadyClaimedException(taskId);
        }

        task.assignTo(currentUserService.require());
    }

    // The request closes on its own when the task is done: a developer says the
    // work is finished, and the customer's request has no separate life after
    // that. No owner approval step, because there is no reviewer role here to
    // put behind one.
    @Transactional
    public void advance(Long taskId, WorkflowStatus target) {
        WorkflowEntity task = requireTask(taskId);
        requireOwnership(task);

        task.transitionTo(target);

        if (target == WorkflowStatus.DONE) {
            task.getRequest().markClosed(LocalDateTime.now());
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
    private void requireOwnership(WorkflowEntity task) {
        UserEntity assignee = task.getDeveloper();
        if (assignee == null || !assignee.getId().equals(currentUserService.requireId())) {
            throw new TaskNotAssignedToYouException(task.getId());
        }
    }
}