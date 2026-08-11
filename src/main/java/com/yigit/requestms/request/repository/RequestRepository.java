package com.yigit.requestms.request.repository;

import com.yigit.requestms.request.dto.CustomerRequestDto;
import com.yigit.requestms.request.entity.RequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RequestRepository extends JpaRepository<RequestEntity, Long> {

    // Constructor expression rather than entity loading: one query regardless of
    // row count, and the CLOB description never leaves the database.
    @Query("""
            SELECT new com.yigit.requestms.request.dto.CustomerRequestDto(
                r.id, r.title, r.status, r.rejectionReason, r.createdAt)
            FROM RequestEntity r
            WHERE r.customer.id = :customerId
            """)
    Page<CustomerRequestDto> findSummariesByCustomer(@Param("customerId") Long customerId,
                                                     Pageable pageable);

    long countByCustomerId(Long customerId);

    // Ownership is part of the query, not a check performed after loading: a
    // customer asking for someone else's request gets an empty result rather
    // than a row the service then has to remember to reject.
    Optional<RequestEntity> findByIdAndCustomerId(Long id, Long customerId);
}