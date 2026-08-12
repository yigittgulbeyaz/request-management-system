package com.yigit.requestms.workflow.service;

import com.yigit.requestms.request.entity.RequestEntity;
import com.yigit.requestms.request.enums.RequestStatus;
import com.yigit.requestms.request.exception.RequestNotFoundException;
import com.yigit.requestms.request.repository.RequestRepository;
import com.yigit.requestms.workflow.entity.WorkflowEntity;
import com.yigit.requestms.workflow.exception.WorkflowAlreadyExistsException;
import com.yigit.requestms.workflow.repository.WorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowService {

    private final RequestRepository requestRepository;
    private final WorkflowRepository workflowRepository;

    public WorkflowService(RequestRepository requestRepository,
                           WorkflowRepository workflowRepository) {
        this.requestRepository = requestRepository;
        this.workflowRepository = workflowRepository;
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
}