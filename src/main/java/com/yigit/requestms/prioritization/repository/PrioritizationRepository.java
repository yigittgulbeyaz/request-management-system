package com.yigit.requestms.prioritization.repository;

import com.yigit.requestms.prioritization.entity.PrioritizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrioritizationRepository extends JpaRepository<PrioritizationEntity, Long> {

    Optional<PrioritizationEntity> findByRequestId(Long requestId);
}