package com.yigit.requestms.workflow.service;

import com.yigit.requestms.common.security.CurrentUserService;
import com.yigit.requestms.request.entity.RequestEntity;
import com.yigit.requestms.request.enums.RequestStatus;
import com.yigit.requestms.request.repository.RequestRepository;
import com.yigit.requestms.request.service.RequestAuditService;
import com.yigit.requestms.user.entity.UserEntity;
import com.yigit.requestms.user.enums.Role;
import com.yigit.requestms.user.enums.SecurityQuestion;
import com.yigit.requestms.user.repository.UserRepository;
import com.yigit.requestms.workflow.entity.WorkflowEntity;
import com.yigit.requestms.workflow.exception.TaskAlreadyClaimedException;
import com.yigit.requestms.workflow.repository.WorkflowRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

// Runs against the real Oracle rather than an in-memory database. Row locking
// is the subject, and a database that only pretends to lock would let this test
// pass while production still lost writes.
//
// The rows it writes are removed afterwards. Nothing here is visible to a user,
// but this test is not read-only.
@SpringBootTest
@ActiveProfiles("test")
class WorkflowConcurrencyTest {

    private static final String FIXTURE_TITLE = "Concurrency fixture: contended task";

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private CurrentUserService currentUserService;

    // The trail is not what this test is about, and writing it would add rows
    // to the contention being measured.
    @MockitoBean
    private RequestAuditService auditService;

    // Each thread records who it stands in for, so one stub answers both
    // without either overwriting the other. Stubbing per thread would mean
    // holding a lock around the call, and a call under a lock is not a race.
    private final ThreadLocal<UserEntity> actingAs = new ThreadLocal<>();

    private UserEntity customer;
    private UserEntity firstDeveloper;
    private UserEntity secondDeveloper;
    private Long taskId;

    @BeforeEach
    void setUp() {
        customer = userRepository.save(
                newUser("Concurrency Customer", "concurrency.customer@test.local", Role.CUSTOMER));
        firstDeveloper = userRepository.save(
                newUser("Concurrency First", "concurrency.first@test.local", Role.DEVELOPER));
        secondDeveloper = userRepository.save(
                newUser("Concurrency Second", "concurrency.second@test.local", Role.DEVELOPER));

        RequestEntity request = new RequestEntity(customer, FIXTURE_TITLE,
                "Two developers will reach for this at the same moment.");
        request.transitionTo(RequestStatus.PRIORITIZED);
        request.transitionTo(RequestStatus.IN_WORKFLOW);
        requestRepository.save(request);

        taskId = workflowRepository.save(new WorkflowEntity(request)).getId();

        Mockito.when(currentUserService.require()).thenAnswer(invocation -> actingAs.get());
    }

    // Children before parents: the foreign keys carry no cascade, because
    // nothing in this system is deleted in normal use.
    @AfterEach
    void removeWhatTheTestWrote() {
        workflowRepository.findById(taskId).ifPresent(workflowRepository::delete);

        requestRepository.findAll().stream()
                .filter(request -> FIXTURE_TITLE.equals(request.getTitle()))
                .forEach(requestRepository::delete);

        userRepository.deleteAll(List.of(firstDeveloper, secondDeveloper, customer));
        actingAs.remove();
    }

    @Test
    @DisplayName("only one of two simultaneous claims succeeds")
    void concurrentClaimsProduceOneWinner() throws Exception {
        // Both threads are held at the same line and released together, so the
        // race is arranged rather than hoped for. Left to chance they would run
        // in sequence and the test would pass for the wrong reason.
        CountDownLatch startTogether = new CountDownLatch(1);
        CountDownLatch bothFinished = new CountDownLatch(2);

        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        AtomicReference<Throwable> unexpected = new AtomicReference<>();

        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            threads.submit(claimAs(firstDeveloper, startTogether, bothFinished,
                    succeeded, rejected, unexpected));
            threads.submit(claimAs(secondDeveloper, startTogether, bothFinished,
                    succeeded, rejected, unexpected));

            startTogether.countDown();

            assertThat(bothFinished.await(30, TimeUnit.SECONDS))
                    .as("both attempts should finish; a hang here means a lock was never released")
                    .isTrue();
        } finally {
            threads.shutdownNow();
        }

        assertThat(unexpected.get())
                .as("a failure other than the task being taken means the lock is not doing its job")
                .isNull();

        // Without the row lock both threads read an unclaimed task, both pass
        // the check, and the second write silently replaces the first: two
        // winners, and the loser never told they lost.
        assertThat(succeeded.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(1);

        WorkflowEntity stored = workflowRepository.findById(taskId).orElseThrow();
        assertThat(stored.getDeveloper()).isNotNull();
        assertThat(stored.getAssignedAt()).isNotNull();
    }

    private Runnable claimAs(UserEntity developer,
                             CountDownLatch startTogether, CountDownLatch bothFinished,
                             AtomicInteger succeeded, AtomicInteger rejected,
                             AtomicReference<Throwable> unexpected) {
        return () -> {
            try {
                actingAs.set(developer);
                startTogether.await();

                // No lock around this call. Both threads enter claim() at the
                // same moment, which is the point: what keeps them apart has to
                // be the row lock, not anything arranged here.
                workflowService.claim(taskId);
                succeeded.incrementAndGet();
            } catch (TaskAlreadyClaimedException e) {
                rejected.incrementAndGet();
            } catch (Throwable e) {
                unexpected.set(e);
            } finally {
                actingAs.remove();
                bothFinished.countDown();
            }
        };
    }

    private UserEntity newUser(String name, String email, Role role) {
        return new UserEntity(name, email, "hash", role,
                SecurityQuestion.BIRTH_CITY, "answerHash");
    }
}