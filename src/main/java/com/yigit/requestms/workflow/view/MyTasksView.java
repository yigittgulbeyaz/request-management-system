package com.yigit.requestms.workflow.view;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.yigit.requestms.common.ui.MainLayout;
import com.yigit.requestms.request.ui.PoStatusPresentation;
import com.yigit.requestms.workflow.dto.TaskSummaryDto;
import com.yigit.requestms.workflow.enums.WorkflowStatus;
import com.yigit.requestms.workflow.service.WorkflowService;
import com.yigit.requestms.workflow.ui.DeadlinePresentation;
import com.yigit.requestms.workflow.ui.WorkflowStatusPresentation;
import jakarta.annotation.security.RolesAllowed;

import java.util.Comparator;
import java.util.List;

@Route(value = "dev/tasks", layout = MainLayout.class)
@PageTitle("My Tasks")
@RolesAllowed("DEVELOPER")
public class MyTasksView extends VerticalLayout {

    private final WorkflowService workflowService;
    private final Grid<TaskSummaryDto> grid = new Grid<>();
    private final Tabs tabs = new Tabs();
    private final Span overdueBanner = new Span();

    // Null means every stage; the tabs carry the filter rather than a separate
    // control, because a developer is always looking at one stage at a time.
    private WorkflowStatus selectedStatus;

    public MyTasksView(WorkflowService workflowService) {
        this.workflowService = workflowService;

        setSizeFull();
        configureBanner();
        configureTabs();
        configureGrid();

        add(new H2("My tasks"), overdueBanner, tabs, grid);
    }

    private void configureBanner() {
        overdueBanner.getStyle()
                .set("color", "#a52020")
                .set("background-color", "#fdeaea")
                .set("border-radius", "6px")
                .set("padding", "0.6em 1em")
                .set("font-weight", "500")
                .set("display", "inline-block");
        overdueBanner.setVisible(false);
    }

    private void refreshBanner() {
        long overdue = workflowService.listMyTasks(null, org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .filter(TaskSummaryDto::isOverdue)
                .count();

        overdueBanner.setVisible(overdue > 0);
        overdueBanner.setText(overdue == 1
                ? "1 task is past its deadline."
                : overdue + " tasks are past their deadline.");
    }

    private void configureTabs() {
        Tab all = new Tab("All");
        tabs.add(all);
        for (WorkflowStatus status : WorkflowStatus.values()) {
            tabs.add(new Tab(WorkflowStatusPresentation.label(status)));
        }

        tabs.addSelectedChangeListener(e -> {
            int index = tabs.getSelectedIndex();
            selectedStatus = index == 0 ? null : WorkflowStatus.values()[index - 1];
            grid.getDataProvider().refreshAll();
        });
    }

    private void configureGrid() {
        grid.addColumn(TaskSummaryDto::requestTitle)
                .setHeader("Task")
                .setFlexGrow(1);

        grid.addComponentColumn(dto -> PoStatusPresentation.scoreBadge(dto.priorityScore()))
                .setHeader("Score")
                .setWidth("170px")
                .setFlexGrow(0);

        grid.addComponentColumn(DeadlinePresentation::badge)
                .setHeader("Due")
                .setWidth("200px")
                .setFlexGrow(0);

        grid.addComponentColumn(dto -> WorkflowStatusPresentation.badge(dto.status()))
                .setHeader("Stage")
                .setWidth("150px")
                .setFlexGrow(0);

        grid.addComponentColumn(this::actionColumn)
                .setHeader("Move to")
                .setWidth("300px")
                .setFlexGrow(0);

        grid.setSizeFull();
        grid.setEmptyStateText("Nothing here. Take something from the available tasks.");

        // Sorted in memory rather than in the query: a developer's board holds
        // the handful of tasks assigned to one person, and sorting a page that
        // small costs nothing while keeping the ordering rule in one place.
        grid.setItemsPageable(
                pageable -> loadSorted(pageable),
                query -> (int) workflowService.countMyTasks(selectedStatus));

        refreshBanner();
    }

    private List<TaskSummaryDto> loadSorted(org.springframework.data.domain.Pageable pageable) {
        return workflowService.listMyTasks(selectedStatus, pageable).stream()
                .sorted(Comparator.comparing(DeadlinePresentation::sortKey))
                .toList();
    }

    // One button per allowed target, taken from the enum rather than written
    // out per stage. When the rules change the board follows without being
    // edited, and a stage with nowhere to go renders nothing.
    //
    // A task past its deadline keeps every button it had: running late is a
    // reason to finish it, not a reason to be unable to.
    private HorizontalLayout actionColumn(TaskSummaryDto dto) {
        HorizontalLayout actions = new HorizontalLayout();

        for (WorkflowStatus target : dto.status().allowedTransitions()) {
            actions.add(transitionButton(dto, target));
        }
        return actions;
    }

    private Button transitionButton(TaskSummaryDto dto, WorkflowStatus target) {
        String label = WorkflowStatusPresentation.actionLabel(dto.status(), target);

        // DONE is final and closes the customer's request with it, so it is the
        // one move that asks first. Sending a task back for rework is
        // reversible, and confirming everything trains people to confirm
        // without reading.
        ComponentEventListener<ClickEvent<Button>> listener = target.isFinal()
                ? e -> confirmThenAdvance(dto, target)
                : e -> advance(dto, target);

        Button button = new Button(label, listener);
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        if (target.isFinal()) {
            button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        } else if (target == WorkflowStatus.IN_PROGRESS && dto.status() == WorkflowStatus.TESTING) {
            button.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        }
        return button;
    }

    private void confirmThenAdvance(TaskSummaryDto dto, WorkflowStatus target) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Mark as done?");
        dialog.setText("This cannot be reopened, and the customer's request "
                + "closes with it.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Mark as Done");
        dialog.addConfirmListener(e -> advance(dto, target));
        dialog.open();
    }

    private void advance(TaskSummaryDto dto, WorkflowStatus target) {
        workflowService.advance(dto.taskId(), target);
        Notification.show("Moved to " + WorkflowStatusPresentation.label(target),
                        3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        grid.getDataProvider().refreshAll();
        refreshBanner();
    }
}