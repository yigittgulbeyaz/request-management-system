package com.yigit.requestms.admin.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.yigit.requestms.admin.dto.CreatedUserDto;

import java.time.format.DateTimeFormatter;

// Shows the setup code once. Only this dialog ever displays it: an
// administrator who closes it too early has to issue a new one, which is
// cheaper than a screen that can show any account's code on demand.
//
// Saying so here is the point. A code shown without that warning gets closed
// like any other confirmation.
public class SetupCodeDialog extends Dialog {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public SetupCodeDialog(CreatedUserDto created) {
        setHeaderTitle("Setup code");
        setWidth("500px");
        setCloseOnEsc(false);
        setCloseOnOutsideClick(false);

        VerticalLayout content = new VerticalLayout(
                new Paragraph("Give this to " + created.email()
                        + ". They will use it once to choose their password and "
                        + "security question."),
                code(created.setupCode()),
                expiry(created),
                warning());
        content.setPadding(false);

        add(content);
        getFooter().add(closeButton());
    }

    private Span code(String setupCode) {
        Span code = new Span(setupCode);
        code.getStyle()
                .set("font-family", "monospace")
                .set("font-size", "1.6rem")
                .set("letter-spacing", "0.12em")
                .set("background-color", "#edf2f7")
                .set("border-radius", "6px")
                .set("padding", "0.5em 0.8em")
                .set("display", "inline-block");
        return code;
    }

    private Paragraph expiry(CreatedUserDto created) {
        Paragraph expiry = new Paragraph(
                "Valid until " + created.expiresAt().format(DATE_FORMAT)
                        + ". After that a new code has to be issued.");
        expiry.getStyle().set("color", "#4a5568");
        return expiry;
    }

    private Paragraph warning() {
        Paragraph warning = new Paragraph(
                "This is the only time it is shown, and using it destroys it.");
        warning.getStyle().set("color", "#8a6100");
        return warning;
    }

    private Button closeButton() {
        Button close = new Button("I have written it down", e -> close());
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return close;
    }
}