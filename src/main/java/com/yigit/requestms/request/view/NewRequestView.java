package com.yigit.requestms.request.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.yigit.requestms.common.ui.MainLayout;
import com.yigit.requestms.request.dto.RequestCreateDto;
import com.yigit.requestms.request.service.RequestService;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "requests/new", layout = MainLayout.class)
@PageTitle("New Request")
@RolesAllowed("CUSTOMER")
public class NewRequestView extends VerticalLayout {

    private final RequestService requestService;

    private final TextField title = new TextField("Title");
    private final TextArea description = new TextArea("Details");
    private final Binder<RequestCreateDto> binder = new Binder<>(RequestCreateDto.class);

    public NewRequestView(RequestService requestService) {
        this.requestService = requestService;

        setMaxWidth("800px");
        configureFields();
        bindFields();

        add(new H2("Report a new support or development request"),
                new FormLayout(title, description),
                buildActions());
    }

    private void configureFields() {
        title.setPlaceholder("e.g. Credit card error on the payment screen");
        title.setMaxLength(200);
        title.setValueChangeMode(ValueChangeMode.EAGER);
        // Shows the limit before it is hit rather than rejecting on submit.
        title.addValueChangeListener(e ->
                title.setHelperText(e.getValue().length() + " / 200"));

        description.setPlaceholder(
                "Describe the problem or the feature you would like added");
        description.setMaxLength(4000);
        description.setHeight("240px");
        description.setValueChangeMode(ValueChangeMode.EAGER);
        description.addValueChangeListener(e ->
                description.setHelperText(e.getValue().length() + " / 4000"));
    }

    // Binding to the DTO rather than the entity: the entity carries status and
    // customer, which a form must never be able to set.
    private void bindFields() {
        binder.forField(title)
                .asRequired("Title is required")
                .withValidator(v -> v.trim().length() >= 5,
                        "Title must be at least 5 characters")
                .bind(RequestCreateDto::title, null);

        binder.forField(description)
                .asRequired("Details are required")
                .withValidator(v -> v.trim().length() >= 20,
                        "Details must be at least 20 characters")
                .bind(RequestCreateDto::description, null);
    }

    private HorizontalLayout buildActions() {
        Button submit = new Button("Submit Request", e -> submit());
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button clear = new Button("Clear", e -> clear());
        clear.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        return new HorizontalLayout(submit, clear);
    }

    private void submit() {
        // Record fields are immutable, so the binder validates the fields and
        // the DTO is constructed from them rather than written into.
        if (!binder.validate().isOk()) {
            return;
        }

        requestService.submit(new RequestCreateDto(title.getValue(), description.getValue()));

        Notification.show("Your request has been received.", 3000,
                        Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        clear();
        getUI().ifPresent(ui -> ui.navigate("requests/my"));
    }

    private void clear() {
        title.clear();
        description.clear();
    }
}