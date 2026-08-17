package com.yigit.requestms.workflow.ui;

import com.yigit.requestms.common.ui.StatusBadge;
import com.yigit.requestms.workflow.dto.TaskSummaryDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

// How a deadline reads on a board. The date alone says little at a glance:
// what a developer scanning a list needs is which rows are late and which are
// about to be, which is what the colour and the relative wording carry.
public final class DeadlinePresentation {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private DeadlinePresentation() {
    }

    public static StatusBadge badge(TaskSummaryDto task) {
        if (task.deadline() == null) {
            // Tasks converted before the rule existed. Saying so is better than
            // showing a date that was never promised.
            return new StatusBadge("No deadline", StatusBadge.Tone.NEUTRAL);
        }
        return new StatusBadge(label(task), tone(task));
    }

    private static String label(TaskSummaryDto task) {
        String date = task.deadline().format(DATE_FORMAT);

        if (task.status().isFinal()) {
            return date;
        }

        long days = ChronoUnit.DAYS.between(
                LocalDate.now(), task.deadline().toLocalDate());

        if (days < 0) {
            return date + " (" + Math.abs(days) + "d late)";
        }
        if (days == 0) {
            return date + " (today)";
        }
        return date + " (" + days + "d left)";
    }

    private static StatusBadge.Tone tone(TaskSummaryDto task) {
        if (task.isOverdue()) {
            return StatusBadge.Tone.NEGATIVE;
        }
        if (task.isDueSoon()) {
            return StatusBadge.Tone.WARNING;
        }
        // Finished work carries its date without alarm, however late it was:
        // the report counts that, the board no longer needs to shout about it.
        return task.status().isFinal() ? StatusBadge.Tone.POSITIVE : StatusBadge.Tone.NEUTRAL;
    }

    // Nulls last, then soonest first. A task with no deadline is not urgent,
    // it is unmeasured, and pushing it to the bottom keeps it from displacing
    // work that has a date to meet.
    public static LocalDateTime sortKey(TaskSummaryDto task) {
        return task.deadline() == null ? LocalDateTime.MAX : task.deadline();
    }
}