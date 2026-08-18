package com.yigit.requestms.admin.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.yigit.requestms.admin.dto.CreatedUserDto;

// Shows the temporary password once. Only the hash is stored, so there is no
// screen that can show it again and no query that can recover it: an
// administrator who closes this dialog too early has to issue a new one.
//
// Saying so in the dialog is the point. A password shown without that warning
// gets closed like any other confirmation.
public class TemporaryPasswordDialog extends Dialog {

    public TemporaryPasswordDialog(CreatedUserDto created) {
        setHeaderTitle("Temporary password");
        setWidth("480px");
        setCloseOnEsc(false);
        setCloseOnOutsideClick(false);

        Span password = new Span(created.temporaryPassword());
        password.getStyle()
                .set("font-family", "monospace")
                .set("font-size", "1.35rem")
                .set("letter-spacing", "0.08em")
                .set("background-color", "#edf2f7")
                .set("border-radius", "6px")
                .set("padding", "0.5em 0.8em")
                .set("display", "inline-block");

        VerticalLayout content = new VerticalLayout(
                new Paragraph("Give this to " + created.email()
                        + ". They will be asked to replace it when they sign in."),
                password,
                warning());
        content.setPadding(false);

        add(content);
        getFooter().add(closeButton());
    }

    private Paragraph warning() {
        Paragraph warning = new Paragraph(
                "This is the only time it is shown. Nothing stores it, so it "
                        + "cannot be looked up later.");
        warning.getStyle().set("color", "#8a6100");
        return warning;
    }

    private Button closeButton() {
        Button close = new Button("I have copied it", e -> close());
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return close;
    }
}