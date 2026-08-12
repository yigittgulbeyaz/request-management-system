package com.yigit.requestms.request.repository;

import com.yigit.requestms.request.dto.CustomerRequestDto;
import com.yigit.requestms.request.dto.RequestSummaryDto;
import com.yigit.requestms.request.entity.RequestEntity;
import com.yigit.requestms.request.enums.RequestStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<RequestEntity, Long> {

    // Constructor expression rather than entity loading: one query regardless of
    // row count, and the CLOB description never leaves the database.
    //
    // List rather than Page because the grid asks for the count separately; a
    // Page would issue a second COUNT query on every fetch for a total that
    // nobody reads.
    @Query("""
            SELECT new com.yigit.requestms.request.dto.CustomerRequestDto(
                r.id, r.title, r.status, r.rejectionReason, r.createdAt)
            FROM RequestEntity r
            WHERE r.customer.id = :customerId
            """)
    List<CustomerRequestDto> findSummariesByCustomer(@Param("customerId") Long customerId,
                                                     Pageable pageable);

    long countByCustomerId(Long customerId);

    // Ownership is part of the query, not a check performed after loading: a
    // customer asking for someone else's request gets an empty result rather
    // than a row the service then has to remember to reject.
    Optional<RequestEntity> findByIdAndCustomerId(Long id, Long customerId);

    // LEFT JOIN is required, not a preference: an inner join would hide exactly
    // the rows the product owner most needs to act on, the unscored ones.
    //
    // The optional status filter is expressed as ":status IS NULL OR ..." so a
    // single query serves both the filtered and unfiltered views.
    //
    // No ORDER BY here on purpose. Ordering arrives with the Pageable, so a
    // column the owner clicks replaces the default rather than queuing behind a
    // clause it can never outrank.
    @Query("""
            SELECT new com.yigit.requestms.request.dto.RequestSummaryDto(
                r.id, u.nameSurname, r.title, p.priorityScore, r.status, r.createdAt)
            FROM RequestEntity r
            JOIN r.customer u
            LEFT JOIN PrioritizationEntity p ON p.request = r
            WHERE (:status IS NULL OR r.status = :status)
            """)
    List<RequestSummaryDto> findPoolSummaries(@Param("status") RequestStatus status,
                                              Pageable pageable);

    @Query("""
            SELECT COUNT(r)
            FROM RequestEntity r
            WHERE (:status IS NULL OR r.status = :status)
            """)
    long countPoolSummaries(@Param("status") RequestStatus status);
}