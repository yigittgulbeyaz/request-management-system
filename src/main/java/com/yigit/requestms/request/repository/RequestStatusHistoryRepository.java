package com.yigit.requestms.request.repository;

import com.yigit.requestms.request.entity.RequestStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestStatusHistoryRepository
        extends JpaRepository<RequestStatusHistoryEntity, Long> {

    List<RequestStatusHistoryEntity> findByRequestIdOrderByChangedAtDesc(Long requestId);
}