package com.yigit.requestms.workflow.repository;

import com.yigit.requestms.workflow.entity.WorkflowEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowRepository extends JpaRepository<WorkflowEntity, Long> {

    boolean existsByRequestId(Long requestId);
}