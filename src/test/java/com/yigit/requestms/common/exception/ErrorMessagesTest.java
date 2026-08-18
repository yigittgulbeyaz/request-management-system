package com.yigit.requestms.common.exception;

import com.yigit.requestms.admin.exception.CannotDemoteLastAdminException;
import com.yigit.requestms.prioritization.exception.PrioritizationNotEditableException;
import com.yigit.requestms.request.enums.RequestStatus;
import com.yigit.requestms.request.exception.InvalidRequestTransitionException;
import com.yigit.requestms.request.exception.RejectionReasonRequiredException;
import com.yigit.requestms.request.exception.RequestNotFoundException;
import com.yigit.requestms.user.exception.DuplicateEmailException;
import com.yigit.requestms.user.exception.UserNotFoundException;
import com.yigit.requestms.workflow.enums.WorkflowStatus;
import com.yigit.requestms.workflow.exception.InvalidWorkflowTransitionException;
import com.yigit.requestms.workflow.exception.TaskAlreadyClaimedException;
import com.yigit.requestms.workflow.exception.TaskNotAssignedToYouException;
import com.yigit.requestms.workflow.exception.TaskNotFoundException;
import com.yigit.requestms.workflow.exception.WorkflowAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

// An exception with no entry here reaches the user as "something went wrong",
// which is what a defect looks like. Listing every one of them means adding a
// new exception without a message fails the build rather than the user.
class ErrorMessagesTest {

    static Stream<BaseException> everyException() {
        return Stream.of(
                new RequestNotFoundException(1L),
                new InvalidRequestTransitionException(RequestStatus.NEW, RequestStatus.CLOSED),
                new RejectionReasonRequiredException(),
                new PrioritizationNotEditableException(RequestStatus.IN_WORKFLOW),
                new WorkflowAlreadyExistsException(1L),
                new InvalidWorkflowTransitionException(WorkflowStatus.BACKLOG, WorkflowStatus.DONE),
                new TaskNotFoundException(1L),
                new TaskAlreadyClaimedException(1L),
                new TaskNotAssignedToYouException(1L),
                new UnauthenticatedException("no session"),
                new UserNotFoundException(1L),
                new DuplicateEmailException("someone@example.com"),
                new CannotDemoteLastAdminException()
        );
    }

    @ParameterizedTest(name = "{0} has something to say to the user")
    @MethodSource("everyException")
    void everyExceptionHasAMessage(BaseException exception) {
        assertThat(ErrorMessages.hasMessageFor(exception.getErrorCode()))
                .as("no message for %s", exception.getErrorCode())
                .isTrue();
    }

    @ParameterizedTest
    @MethodSource("everyException")
    @DisplayName("the message is written for the user, not copied from the exception")
    void messagesAreNotTheInternalText(BaseException exception) {
        String shown = ErrorMessages.forCode(exception.getErrorCode());

        // The exception's own message names ids and states, which help whoever
        // reads the log and mean nothing to whoever reads the screen.
        assertThat(shown).isNotEqualTo(exception.getMessage());
        assertThat(shown).endsWith(".");
    }

    @Test
    @DisplayName("an unknown code falls back rather than showing the code itself")
    void unknownCodeFallsBack() {
        assertThat(ErrorMessages.forCode("NOT_A_REAL_CODE"))
                .isEqualTo("Something went wrong. Please try again.");
    }

    @Test
    @DisplayName("a null code falls back too")
    void nullCodeFallsBack() {
        // The handler passes null for anything that is not a BaseException,
        // which is to say for defects.
        assertThat(ErrorMessages.forCode(null))
                .isEqualTo("Something went wrong. Please try again.");
    }
}