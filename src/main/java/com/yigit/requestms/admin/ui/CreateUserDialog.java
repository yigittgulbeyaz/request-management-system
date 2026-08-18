package com.yigit.requestms.admin.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.yigit.requestms.admin.dto.CreateUserDto;
import com.yigit.requestms.user.enums.Role;
import com.yigit.requestms.user.enums.SecurityQuestion;

import java.util.function.Consumer;

// No password field. One is generated on the server and shown once, so an
// administrator never chooses a lasting password for someone else and never
// has one to remember on their behalf.
public class CreateUserDialog extends Dialog {

    private final TextField nameSurname = new TextField("Full name");
    private final EmailField email = new EmailField("Email");
    private final Select<Role> role = new Select<>();
    private final Select<SecurityQuestion> question = new Select<>();
    private final TextField answer = new TextField("Security answer");
    private final Button save = new Button("Create user");

    public CreateUserDialog(Consumer<CreateUserDto> onCreate) {
        setHeaderTitle("New user");
        setWidth("560px");

        configureFields();

        FormLayout form = new FormLayout(nameSurname, email, role, question, answer);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        add(form);

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.setEnabled(false);
        save.addClickListener(e -> {
            onCreate.accept(new CreateUserDto(
                    nameSurname.getValue(), email.getValue(), role.getValue(),
                    question.getValue(), answer.getValue()));
            close();
        });

        Button cancel = new Button("Cancel", e -> close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        getFooter().add(cancel, save);
    }

    private void configureFields() {
        nameSurname.setRequired(true);
        nameSurname.setMaxLength(100);
        nameSurname.addValueChangeListener(e -> refreshSaveState());

        email.setRequired(true);
        email.setMaxLength(100);
        email.setErrorMessage("Enter a valid email address");
        email.addValueChangeListener(e -> refreshSaveState());

        role.setLabel("Role");
        role.setItems(Role.values());
        role.setItemLabelGenerator(UserStatePresentation::label);
        role.addValueChangeListener(e -> refreshSaveState());

        // The question is picked from a fixed list rather than typed: free text
        // invites questions with public answers, and a stored question has to be
        // translatable, which a sentence someone typed is not.
        question.setLabel("Security question");
        question.setItems(SecurityQuestion.values());
        question.setItemLabelGenerator(SecurityQuestion::name);
        question.addValueChangeListener(e -> refreshSaveState());

        answer.setRequired(true);
        answer.setHelperText("Stored hashed. Case and surrounding spaces are ignored.");
        answer.addValueChangeListener(e -> refreshSaveState());
    }

    // Enabled only once every field has something in it, so the failure a user
    // meets first is a missing field on screen rather than a rejected save.
    private void refreshSaveState() {
        save.setEnabled(
                !nameSurname.isEmpty() && !email.isEmpty() && !email.isInvalid()
                        && role.getValue() != null && question.getValue() != null
                        && !answer.isEmpty());
    }
}