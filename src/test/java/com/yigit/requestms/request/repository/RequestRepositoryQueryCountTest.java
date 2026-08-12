package com.yigit.requestms.request.repository;

import com.yigit.requestms.request.dto.RequestSummaryDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// The point of the projection is that one query serves the whole page. Claiming
// that is easy; this measures it. Query count is asserted rather than duration
// because duration varies with the machine while the count is the property that
// either holds or does not.
@SpringBootTest
@ActiveProfiles("test")
class RequestRepositoryQueryCountTest {

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void resetStatistics() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    @Transactional(readOnly = true)
    @DisplayName("reads a page of the pool in a single query")
    void poolPageCostsOneQuery() {
        List<RequestSummaryDto> page =
                requestRepository.findPoolSummaries(null, PageRequest.of(0, 20));

        assertThat(page).isNotEmpty();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    @Transactional(readOnly = true)
    @DisplayName("costs the same query whether the page holds five rows or thirty")
    void queryCountDoesNotGrowWithPageSize() {
        requestRepository.findPoolSummaries(null, PageRequest.of(0, 5));
        long afterSmallPage = statistics.getPrepareStatementCount();

        statistics.clear();
        requestRepository.findPoolSummaries(null, PageRequest.of(0, 30));
        long afterLargePage = statistics.getPrepareStatementCount();

        // The N+1 this projection avoids would show up here as a count that
        // tracks the row count: six queries for five rows, thirty-one for thirty.
        assertThat(afterSmallPage).isEqualTo(1);
        assertThat(afterLargePage).isEqualTo(1);
    }

    @Test
    @Transactional(readOnly = true)
    @DisplayName("walking the associations instead would cost a query per row")
    void loadingEntitiesShowsTheProblemTheProjectionAvoids() {
        List<Object[]> rows = entityManager.createQuery(
                        "SELECT r.id, r.title FROM RequestEntity r", Object[].class)
                .setMaxResults(10)
                .getResultList();

        statistics.clear();

        // Touching a lazy association per row is the shape the projection
        // replaces. Kept as a test so the difference is demonstrable rather
        // than asserted in a comment.
        entityManager.createQuery(
                        "SELECT r FROM RequestEntity r", com.yigit.requestms.request.entity.RequestEntity.class)
                .setMaxResults(10)
                .getResultList()
                .forEach(request -> request.getCustomer().getNameSurname());

        assertThat(rows).isNotEmpty();
        assertThat(statistics.getPrepareStatementCount()).isGreaterThan(1);
    }
}