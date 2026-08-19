package com.yigit.requestms.user.view;

import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.yigit.requestms.admin.ui.UserStatePresentation;
import com.yigit.requestms.auth.service.PasswordPolicy;
import com.yigit.requestms.common.ui.MainLayout;
import com.yigit.requestms.user.dto.PasswordChangeDto;
import com.yigit.requestms.user.dto.ProfileDto;
import com.yigit.requestms.user.dto.ProfileUpdateDto;
import com.yigit.requestms.user.service.EmailPolicy;
import com.yigit.requestms.user.service.ProfileService;
import jakarta.annotation.security.PermitAll;

// PermitAll rather than a role list: every role has an account, and this is
// where its owner comes to look at it.
@Route(value = "profile", layout = MainLayout.class)
@PageTitle("Profile")
@PermitAll
public class ProfileView extends VerticalLayout {

    private final ProfileService profileService;

    private final TextField nameSurname = new TextField("Full name");
    private final EmailField email = new EmailField("Email");
    private final TextField role = new TextField("Role");

    private final PasswordField currentPassword = new PasswordField("Current password");
    private final PasswordField newPassword = new PasswordField("New password");
    private final PasswordField confirmation = new PasswordField("Confirm new password");

    public ProfileView(ProfileService profileService) {
        this.profileService = profileService;

        setMaxWidth("640px");
        configureFields();

        add(new H2("Your account"), detailsSection(), passwordSection());

        render(profileService.load());
    }

    private void configureFields() {
        nameSurname.setRequiredIndicatorVisible(true);
        nameSurname.setMaxLength(100);
        nameSurname.addValueChangeListener(e -> nameSurname.setInvalid(false));

        email.setRequiredIndicatorVisible(true);
        email.setMaxLength(100);
        email.addValueChangeListener(e -> email.setInvalid(false));

        // Read-only rather than absent: knowing what you are is useful, and the
        // field being visibly locked says who decides it better than its
        // absence would.
        role.setReadOnly(true);
        role.setHelperText("Only an administrator can change this.");

        currentPassword.setRequiredIndicatorVisible(true);
        currentPassword.addValueChangeListener(e -> currentPassword.setInvalid(false));

        newPassword.setRequiredIndicatorVisible(true);
        newPassword.setHelperText(PasswordPolicy.describe());
        newPassword.addValueChangeListener(e -> newPassword.setInvalid(false));

        confirmation.setRequiredIndicatorVisible(true);
        confirmation.addValueChangeListener(e -> confirmation.setInvalid(false));
    }

    private VerticalLayout detailsSection() {
        FormLayout form = new FormLayout(nameSurname, email, role);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button save = new Button("Save changes", e -> saveDetails());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout section = new VerticalLayout(new H3("Details"), form, save);
        section.setPadding(false);
        return section;
    }

    // Collapsed and separate, because changing a password is a different act
    // from correcting a name and answers to a different failure.
    private Details passwordSection() {
        FormLayout form = new FormLayout(currentPassword, newPassword, confirmation);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button change = new Button("Change password", e -> changePassword());
        change.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout content = new VerticalLayout(form, change);
        content.setPadding(false);

        Details details = new Details("Change password", content);
        details.setWidthFull();
        return details;
    }

    private void render(ProfileDto profile) {
        nameSurname.setValue(profile.nameSurname());
        email.setValue(profile.email());
        role.setValue(UserStatePresentation.label(profile.role()));
    }

    private void saveDetails() {
        if (!validateDetails()) {
            return;
        }

        profileService.update(new ProfileUpdateDto(nameSurname.getValue(), email.getValue()));
        notifySuccess("Your details are saved.");
    }

    private boolean validateDetails() {
        boolean valid = true;

        if (nameSurname.isEmpty()) {
            valid = fail(nameSurname, "Enter your name");
        }
        if (email.isEmpty()) {
            valid = fail(email, "Enter an email address");
        } else if (!EmailPolicy.isWellFormed(email.getValue())) {
            valid = fail(email, EmailPolicy.describe());
        }
        return valid;
    }

    private void changePassword() {
        if (!validatePassword()) {
            return;
        }

        profileService.changePassword(
                new PasswordChangeDto(currentPassword.getValue(), newPassword.getValue()));

        currentPassword.clear();
        newPassword.clear();
        confirmation.clear();
        notifySuccess("Your password is changed.");
    }

    private boolean validatePassword() {
        boolean valid = true;

        if (currentPassword.isEmpty()) {
            valid = fail(currentPassword, "Enter your current password");
        }

        if (newPassword.isEmpty()) {
            valid = fail(newPassword, "Choose a new password");
        } else if (!PasswordPolicy.isAcceptable(newPassword.getValue())) {
            valid = fail(newPassword, PasswordPolicy.describe());
        }

        if (confirmation.isEmpty()) {
            valid = fail(confirmation, "Type the new password again");
        } else if (!newPassword.getValue().equals(confirmation.getValue())) {
            valid = fail(confirmation, "The two passwords do not match");
        }
        return valid;
    }

    private void notifySuccess(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private boolean fail(HasValidation field, String message) {
        field.setErrorMessage(message);
        field.setInvalid(true);
        return false;
    }
}