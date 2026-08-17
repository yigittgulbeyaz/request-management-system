package com.yigit.requestms.common.security;

import com.yigit.requestms.prioritization.dto.PrioritizationFormDto;
import com.yigit.requestms.prioritization.enums.ImpactLevel;
import com.yigit.requestms.prioritization.enums.UrgencyLevel;
import com.yigit.requestms.prioritization.service.PrioritizationService;
import com.yigit.requestms.request.dto.RequestCreateDto;
import com.yigit.requestms.request.service.PoRequestService;
import com.yigit.requestms.request.service.RequestService;
import com.yigit.requestms.workflow.enums.WorkflowStatus;
import com.yigit.requestms.workflow.service.WorkflowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The permission matrix in docs/01-roles-and-use-cases.md, asserted rather than
// described. Every case here is a call that should be refused: what a role may
// do is covered by the tests of that feature, what it may not do is only
// covered here.
//
// Nothing reaches a database. Authorization is decided before the method body
// runs, so a refused call never gets far enough to need one.
@SpringBootTest
@ActiveProfiles("test")
class RoleAccessTest {

    @Autowired
    private RequestService requestService;

    @Autowired
    private PoRequestService poRequestService;

    @Autowired
    private PrioritizationService prioritizationService;

    @Autowired
    private WorkflowService workflowService;

    private static final PageRequest FIRST_PAGE = PageRequest.of(0, 20);

    private static final RequestCreateDto A_REQUEST = new RequestCreateDto(
            "Access control fixture",
            "Never submitted, because the call is refused before the body runs.");

    private static final PrioritizationFormDto A_SCORE = new PrioritizationFormDto(
            ImpactLevel.MODERATE, UrgencyLevel.SHORT_TERM);

    @Nested
    @DisplayName("a customer")
    @WithMockUser(roles = "CUSTOMER")
    class AsCustomer {

        @Test
        @DisplayName("cannot read the prioritization pool")
        void cannotReadThePool() {
            assertThatThrownBy(() -> poRequestService.listPool(null, FIRST_PAGE))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("cannot score a request")
        void cannotScore() {
            // The whole point of hiding the score from customers is undone if
            // they can set it.
            assertThatThrownBy(() -> prioritizationService.score(1L, A_SCORE))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("cannot reject a request")
        void cannotReject() {
            assertThatThrownBy(() -> poRequestService.reject(1L, "no"))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("cannot schedule their own request for development")
        void cannotConvertToWorkflow() {
            assertThatThrownBy(() -> workflowService.convertToWorkflow(1L))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("cannot claim development work")
        void cannotClaimTasks() {
            assertThatThrownBy(() -> workflowService.claim(1L))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("a product owner")
    @WithMockUser(roles = "PRODUCT_OWNER")
    class AsProductOwner {

        @Test
        @DisplayName("cannot submit a request on a customer's behalf")
        void cannotSubmitRequests() {
            assertThatThrownBy(() -> requestService.submit(A_REQUEST))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("cannot claim a task")
        void cannotClaimTasks() {
            // Owners decide what gets built, developers decide who builds it.
            assertThatThrownBy(() -> workflowService.claim(1L))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("cannot move a task through its stages")
        void cannotAdvanceTasks() {
            assertThatThrownBy(() -> workflowService.advance(1L, WorkflowStatus.IN_PROGRESS))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("cannot read a developer's board")
        void cannotReadTheDeveloperBoard() {
            assertThatThrownBy(() -> workflowService.listMyTasks(null, FIRST_PAGE))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("a developer")
    @WithMockUser(roles = "DEVELOPER")
    class AsDeveloper {

        @Test
        @DisplayName("cannot read the prioritization pool")
        void cannotReadThePool() {
            assertThatThrownBy(() -> poRequestService.listPool(null, FIRST_PAGE))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("cannot score a request")
        void cannotScore() {
            assertThatThrownBy(() -> prioritizationService.score(1L, A_SCORE))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("cannot schedule work for themselves")
        void cannotConvertToWorkflow() {
            // Otherwise a developer could pull a request past the owner and
            // straight onto the board.
            assertThatThrownBy(() -> workflowService.convertToWorkflow(1L))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("cannot submit a request")
        void cannotSubmitRequests() {
            assertThatThrownBy(() -> requestService.submit(A_REQUEST))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("an administrator")
    @WithMockUser(roles = "ADMIN")
    class AsAdmin {

        @Test
        @DisplayName("cannot score a request")
        void cannotScore() {
            // Admin sees everything and decides none of it. A role that can
            // step around the state machine makes the state machine advisory.
            assertThatThrownBy(() -> prioritizationService.score(1L, A_SCORE))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("cannot schedule a request for development")
        void cannotConvertToWorkflow() {
            assertThatThrownBy(() -> workflowService.convertToWorkflow(1L))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("cannot move a task through its stages")
        void cannotAdvanceTasks() {
            assertThatThrownBy(() -> workflowService.advance(1L, WorkflowStatus.DONE))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("cannot reject a request")
        void cannotReject() {
            assertThatThrownBy(() -> poRequestService.reject(1L, "no"))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("nobody signed in")
    @WithAnonymousUser
    class AsAnonymous {

        @Test
        @DisplayName("cannot submit a request")
        void cannotSubmitRequests() {
            assertThatThrownBy(() -> requestService.submit(A_REQUEST))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("cannot read the prioritization pool")
        void cannotReadThePool() {
            assertThatThrownBy(() -> poRequestService.listPool(null, FIRST_PAGE))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("cannot claim a task")
        void cannotClaimTasks() {
            assertThatThrownBy(() -> workflowService.claim(1L))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }
}