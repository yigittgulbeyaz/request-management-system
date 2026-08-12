package com.yigit.requestms.request.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.yigit.requestms.common.ui.MainLayout;
import com.yigit.requestms.request.dto.RequestSummaryDto;
import com.yigit.requestms.request.enums.RequestStatus;
import com.yigit.requestms.request.service.PoRequestService;
import com.yigit.requestms.request.ui.PoStatusPresentation;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;

@Route(value = "po/pool", layout = MainLayout.class)
@PageTitle("Prioritization Pool")
@RolesAllowed("PRODUCT_OWNER")
public class PrioritizationPoolView extends VerticalLayout {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final PoRequestService poRequestService;
    private final Grid<RequestSummaryDto> grid = new Grid<>();
    private final Select<RequestStatus> statusFilter = new Select<>();

    public PrioritizationPoolView(PoRequestService poRequestService) {
        this.poRequestService = poRequestService;

        setSizeFull();
        configureFilter();
        configureGrid();

        add(new H2("Customer requests awaiting prioritization"),
                new Paragraph("Ordered by impact x urgency, highest first. "
                        + "Requests with no score yet appear last. "
                        + "Click a column header to sort by it instead."),
                new HorizontalLayout(statusFilter),
                grid);
    }

    private void configureFilter() {
        statusFilter.setLabel("Status");
        statusFilter.setItems(RequestStatus.values());
        statusFilter.setEmptySelectionAllowed(true);
        statusFilter.setEmptySelectionCaption("All");
        statusFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
    }

    // Sort properties name entity paths through the query's own aliases, not the
    // DTO fields: sorting happens in the database, so it has to speak the
    // query's vocabulary. Using u.nameSurname rather than customer.nameSurname
    // also reuses the existing join instead of provoking a second one.
    private void configureGrid() {
        grid.addColumn(RequestSummaryDto::id)
                .setHeader("ID")
                .setSortProperty("r.id")
                .setWidth("90px")
                .setFlexGrow(0);

        grid.addColumn(RequestSummaryDto::customerName)
                .setHeader("Customer")
                .setSortProperty("u.nameSurname")
                .setWidth("200px")
                .setFlexGrow(0);

        grid.addColumn(RequestSummaryDto::title)
                .setHeader("Title")
                .setSortProperty("r.title")
                .setFlexGrow(1);

        grid.addComponentColumn(dto -> PoStatusPresentation.scoreBadge(dto.priorityScore()))
                .setHeader("Score")
                .setSortProperty("p.priorityScore")
                .setWidth("170px")
                .setFlexGrow(0);

        // No sort property: sorting statuses alphabetically puts CLOSED first
        // and NEW third, which is no order anyone wants. The status filter above
        // is how the owner narrows to one status.
        grid.addComponentColumn(dto -> PoStatusPresentation.statusBadge(dto.status()))
                .setHeader("Status")
                .setWidth("150px")
                .setFlexGrow(0);

        grid.addColumn(dto -> dto.createdAt().format(DATE_FORMAT))
                .setHeader("Submitted")
                .setSortProperty("r.createdAt")
                .setWidth("130px")
                .setFlexGrow(0);

        // No sort property: the action column renders buttons, not a value.
        grid.addComponentColumn(this::actionColumn)
                .setHeader("Action")
                .setWidth("220px")
                .setFlexGrow(0);

        grid.setSizeFull();
        grid.setEmptyStateText("No requests match this filter.");

        grid.setItemsPageable(
                pageable -> poRequestService.listPool(statusFilter.getValue(), pageable),
                query -> (int) poRequestService.countPool(statusFilter.getValue()));
    }

    // Which action is offered follows from the request's state rather than from
    // a per-row decision here: a request that cannot move has no button.
    private HorizontalLayout actionColumn(RequestSummaryDto dto) {
        HorizontalLayout actions = new HorizontalLayout();

        switch (dto.status()) {
            case NEW -> {
                actions.add(primary("Prioritize"));
                actions.add(danger("Reject"));
            }
            case PRIORITIZED -> {
                actions.add(primary("Convert to Workflow"));
                actions.add(tertiary("Edit"));
            }
            default -> {
                // IN_WORKFLOW, CLOSED and REJECTED are read-only for the owner.
            }
        }
        return actions;
    }

    private Button primary(String label) {
        Button button = new Button(label);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        return button;
    }

    private Button tertiary(String label) {
        Button button = new Button(label);
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        return button;
    }

    private Button danger(String label) {
        Button button = new Button(label);
        button.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY,
                ButtonVariant.LUMO_SMALL);
        return button;
    }
}