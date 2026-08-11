package com.yigit.requestms.request.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.yigit.requestms.common.ui.MainLayout;
import com.yigit.requestms.request.dto.CustomerRequestDto;
import com.yigit.requestms.request.service.RequestService;
import com.yigit.requestms.request.ui.CustomerStatusPresentation;
import com.yigit.requestms.request.ui.RequestDetailDialog;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;

@Route(value = "requests/my", layout = MainLayout.class)
@PageTitle("My Requests")
@RolesAllowed("CUSTOMER")
public class MyRequestsView extends VerticalLayout {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final RequestService requestService;
    private final Grid<CustomerRequestDto> grid = new Grid<>();

    public MyRequestsView(RequestService requestService) {
        this.requestService = requestService;

        setSizeFull();
        configureGrid();
        add(new H2("My requests"), grid);
    }

    private void configureGrid() {
        grid.addColumn(CustomerRequestDto::title)
                .setHeader("Title")
                .setFlexGrow(1);

        grid.addColumn(dto -> dto.createdAt().format(DATE_FORMAT))
                .setHeader("Submitted")
                .setWidth("140px")
                .setFlexGrow(0);

        grid.addComponentColumn(dto -> CustomerStatusPresentation.badge(dto.status()))
                .setHeader("Status")
                .setWidth("220px")
                .setFlexGrow(0);

        grid.addComponentColumn(this::detailButton)
                .setWidth("120px")
                .setFlexGrow(0);

        grid.setSizeFull();
        grid.setEmptyStateText("You haven't submitted any requests yet.");

        // Count callback supplied alongside the fetch: without it the grid does
        // not know the total and the scrollbar recalculates as the user scrolls.
        grid.setItemsPageable(requestService::listMyRequests,
                query -> (int) requestService.countMyRequests());
    }

    private Button detailButton(CustomerRequestDto dto) {
        Button button = new Button("Details",
                e -> new RequestDetailDialog(requestService.getMyRequest(dto.id())).open());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        return button;
    }
}