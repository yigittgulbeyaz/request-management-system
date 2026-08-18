package com.yigit.requestms.admin.ui;

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
        save.setEnabled(false);
        save.addClickListener(e -> {
            onCreate.accept(new CreateUserDto(
                    nameSurname.getValue(), email.getValue(), role.getValue()));
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
    }

    // Enabled only once every field has something in it, so the failure a user
    // meets first is a missing field on screen rather than a rejected save.
    private void refreshSaveState() {
        save.setEnabled(!nameSurname.isEmpty() && !email.isEmpty()
                && !email.isInvalid() && role.getValue() != null);
    }
}