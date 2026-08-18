package com.yigit.requestms.admin.ui;

import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.yigit.requestms.admin.dto.CreateUserDto;
import com.yigit.requestms.user.enums.Role;

import java.util.function.Consumer;

// Three fields: who the person is and what they may do. Nothing about how they
// will prove the account is theirs.
//
// No password and no security question, because an administrator who chose
// either would hold the account open indefinitely. What comes back instead is
// a code to hand over, and the person who uses it chooses both.
public class CreateUserDialog extends Dialog {

    private final TextField nameSurname = new TextField("Full name");
    private final EmailField email = new EmailField("Email");
    private final Select<Role> role = new Select<>();
    private final Button save = new Button("Create account");

    public CreateUserDialog(Consumer<CreateUserDto> onCreate) {
        setHeaderTitle("New user");
        setWidth("520px");

        configureFields();

        FormLayout form = new FormLayout(nameSurname, email, role);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Paragraph note = new Paragraph(
                "A setup code is generated and shown once. Give it to the person "
                        + "and they will choose their own password and security "
                        + "question the first time they sign in.");
        note.getStyle().set("color", "#4a5568");

        VerticalLayout content = new VerticalLayout(form, note);
        content.setPadding(false);
        add(content);

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> submit(onCreate));

        Button cancel = new Button("Cancel", e -> close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        getFooter().add(cancel, save);
    }

    private void configureFields() {
        nameSurname.setRequiredIndicatorVisible(true);
        nameSurname.setMaxLength(100);
        nameSurname.addValueChangeListener(e -> nameSurname.setInvalid(false));

        email.setRequiredIndicatorVisible(true);
        email.setMaxLength(100);
        email.addValueChangeListener(e -> email.setInvalid(false));

        role.setLabel("Role");
        role.setItems(Role.values());
        role.setItemLabelGenerator(UserStatePresentation::label);
        role.setRequiredIndicatorVisible(true);
        role.addValueChangeListener(e -> role.setInvalid(false));
    }

    // The button stays enabled and the form answers on submit. A disabled
    // button with no explanation leaves someone guessing which field it is
    // waiting on, and the guess is usually wrong.
    private void submit(Consumer<CreateUserDto> onCreate) {
        if (!validate()) {
            return;
        }

        onCreate.accept(new CreateUserDto(
                nameSurname.getValue(), email.getValue(), role.getValue()));
        close();
    }

    // Every field is checked rather than stopping at the first, so someone who
    // got two things wrong finds out about both at once.
    private boolean validate() {
        boolean valid = true;

        if (nameSurname.isEmpty()) {
            valid = fail(nameSurname, "Enter the person's name");
        }

        if (email.isEmpty()) {
            valid = fail(email, "Enter an email address");
        } else if (email.isInvalid()) {
            valid = fail(email, "That does not look like an email address");
        }

        if (role.getValue() == null) {
            valid = fail(role, "Pick a role");
        }

        return valid;
    }

    private boolean fail(HasValidation field, String message) {
        field.setErrorMessage(message);
        field.setInvalid(true);
        return false;
    }
}