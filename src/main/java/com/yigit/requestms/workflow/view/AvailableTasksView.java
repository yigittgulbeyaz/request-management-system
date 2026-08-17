package com.yigit.requestms.workflow.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.yigit.requestms.common.ui.MainLayout;
import com.yigit.requestms.request.ui.PoStatusPresentation;
import com.yigit.requestms.workflow.dto.TaskSummaryDto;
import com.yigit.requestms.workflow.service.WorkflowService;
import com.yigit.requestms.workflow.ui.DeadlinePresentation;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;

@Route(value = "dev/available", layout = MainLayout.class)
@PageTitle("Available Tasks")
@RolesAllowed("DEVELOPER")
public class AvailableTasksView extends VerticalLayout {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final WorkflowService workflowService;
    private final Grid<TaskSummaryDto> grid = new Grid<>();

    public AvailableTasksView(WorkflowService workflowService) {
        this.workflowService = workflowService;

        setSizeFull();
        configureGrid();

        add(new H2("Unassigned tasks"),
                new Paragraph("Claiming a task assigns it to you. The deadline "
                        + "was set when the work was scheduled, so it is already "
                        + "running."),
                grid);
    }

    private void configureGrid() {
        grid.addColumn(TaskSummaryDto::requestTitle)
                .setHeader("Task")
                .setFlexGrow(1);

        grid.addComponentColumn(dto -> PoStatusPresentation.scoreBadge(dto.priorityScore()))
                .setHeader("Score")
                .setWidth("170px")
                .setFlexGrow(0);

        // The deadline is here as well as on the board, because it is part of
        // what a developer is agreeing to by claiming: one of these may be a
        // week away and another already late.
        grid.addComponentColumn(DeadlinePresentation::badge)
                .setHeader("Due")
                .setWidth("200px")
                .setFlexGrow(0);

        grid.addColumn(dto -> dto.createdAt().format(DATE_FORMAT))
                .setHeader("Waiting since")
                .setWidth("150px")
                .setFlexGrow(0);

        grid.addComponentColumn(this::claimButton)
                .setWidth("120px")
                .setFlexGrow(0);

        grid.setSizeFull();
        grid.setEmptyStateText("No unassigned tasks right now.");

        grid.setItemsPageable(
                pageable -> workflowService.listUnclaimed(pageable).stream()
                        .sorted(Comparator.comparing(DeadlinePresentation::sortKey))
                        .toList(),
                query -> (int) workflowService.countUnclaimed());
    }

    private Button claimButton(TaskSummaryDto dto) {
        Button button = new Button("Claim", e -> claim(dto));
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        return button;
    }

    private void claim(TaskSummaryDto dto) {
        workflowService.claim(dto.taskId());
        Notification.show("Task claimed.", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        grid.getDataProvider().refreshAll();
    }
}