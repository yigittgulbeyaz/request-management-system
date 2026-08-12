package com.yigit.requestms.workflow.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

// The board's rules live here, so this is where they are pinned down. Every
// pair is stated explicitly rather than only the interesting ones: a rule that
// is not asserted is a rule that can be changed by accident.
class WorkflowStatusTest {

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "BACKLOG, IN_PROGRESS",
            "IN_PROGRESS, TESTING",
            "TESTING, DONE",
            "TESTING, IN_PROGRESS"
    })
    void allowsTheseTransitions(WorkflowStatus from, WorkflowStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} is refused")
    @CsvSource({
            // Skipping stages: nothing is built without being started, and
            // nothing is done without being tested.
            "BACKLOG, TESTING",
            "BACKLOG, DONE",
            "IN_PROGRESS, DONE",
            // Backwards past the one reverse move the board allows.
            "IN_PROGRESS, BACKLOG",
            "TESTING, BACKLOG",
            // Out of the final state.
            "DONE, BACKLOG",
            "DONE, IN_PROGRESS",
            "DONE, TESTING"
    })
    void refusesTheseTransitions(WorkflowStatus from, WorkflowStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(WorkflowStatus.class)
    @DisplayName("no stage can transition to itself")
    void refusesSelfTransition(WorkflowStatus status) {
        assertThat(status.canTransitionTo(status)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(WorkflowStatus.class)
    @DisplayName("a null target is refused rather than throwing")
    void refusesNullTarget(WorkflowStatus status) {
        assertThat(status.canTransitionTo(null)).isFalse();
    }

    @Test
    @DisplayName("DONE is the only final stage")
    void doneIsTheOnlyFinalStage() {
        assertThat(WorkflowStatus.DONE.isFinal()).isTrue();
        assertThat(WorkflowStatus.BACKLOG.isFinal()).isFalse();
        assertThat(WorkflowStatus.IN_PROGRESS.isFinal()).isFalse();
        assertThat(WorkflowStatus.TESTING.isFinal()).isFalse();
    }

    // The developer board renders one button per allowed target, so this set is
    // what the UI will offer. Asserting it here means a change to the rules
    // shows up as a failing test rather than as a missing button.
    @Test
    @DisplayName("TESTING offers both finishing and sending back")
    void testingOffersTwoWaysOut() {
        assertThat(WorkflowStatus.TESTING.allowedTransitions())
                .containsExactlyInAnyOrder(WorkflowStatus.DONE, WorkflowStatus.IN_PROGRESS);
    }
}