package com.yigit.requestms.request.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

import java.util.function.Consumer;

// Rejection is a dead end in the state machine, so the dialog says so before
// the button rather than after. The reason is mandatory and reaches the
// customer, which the helper text states outright: knowing it will be read
// changes how it gets written.
public class RejectRequestDialog extends Dialog {

    private final TextArea reason = new TextArea("Reason for rejection");
    private final Button confirm = new Button("Reject Request");

    public RejectRequestDialog(String requestTitle, Consumer<String> onConfirm) {
        setHeaderTitle("Reject: " + requestTitle);
        setWidth("560px");

        reason.setWidthFull();
        reason.setMinHeight("140px");
        reason.setMaxLength(500);
        reason.setRequired(true);
        reason.setHelperText("This reason will be visible to the customer.");
        reason.addValueChangeListener(e -> confirm.setEnabled(!e.getValue().isBlank()));

        add(new VerticalLayout(
                new Paragraph("This cannot be undone. The request closes for good "
                        + "and the customer is told why."),
                reason));

        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        confirm.setEnabled(false);
        confirm.addClickListener(e -> {
            onConfirm.accept(reason.getValue());
            close();
        });

        Button cancel = new Button("Cancel", e -> close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        getFooter().add(cancel, confirm);
    }
}