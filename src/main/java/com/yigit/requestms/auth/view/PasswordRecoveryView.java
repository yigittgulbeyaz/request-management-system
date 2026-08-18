package com.yigit.requestms.auth.view;

import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.yigit.requestms.auth.dto.PasswordRecoveryDto;
import com.yigit.requestms.auth.dto.RecoveryChallengeDto;
import com.yigit.requestms.auth.service.PasswordPolicy;
import com.yigit.requestms.auth.service.PasswordRecoveryService;

@Route("recover")
@PageTitle("Forgotten password")
@AnonymousAllowed
public class PasswordRecoveryView extends VerticalLayout {

    private final PasswordRecoveryService recoveryService;

    private final EmailField email = new EmailField("Email");
    private final Button findAccount = new Button("Continue");

    private final Paragraph questionLabel = new Paragraph();
    private final TextField answer = new TextField("Your answer");
    private final PasswordField password = new PasswordField("New password");
    private final PasswordField confirmation = new PasswordField("Confirm new password");
    private final Button submit = new Button("Set new password");

    private final VerticalLayout challengeStep = new VerticalLayout();

    public PasswordRecoveryView(PasswordRecoveryService recoveryService) {
        this.recoveryService = recoveryService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        configureFields();

        VerticalLayout card = new VerticalLayout(
                new H2("Forgotten password"),
                new Paragraph("Answer the question you chose when you set up your "
                        + "account, and pick a new password."),
                emailStep(),
                challengeStep,
                new Anchor("login", "Back to sign in"));
        card.setMaxWidth("480px");

        add(card);
    }

    private FormLayout emailStep() {
        findAccount.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        findAccount.addClickListener(e -> loadChallenge());

        FormLayout step = new FormLayout(email, findAccount);
        step.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        return step;
    }

    private void configureFields() {
        email.setRequiredIndicatorVisible(true);
        email.addValueChangeListener(e -> email.setInvalid(false));

        questionLabel.getStyle().set("font-weight", "500");

        answer.setRequiredIndicatorVisible(true);
        answer.setHelperText("Case and surrounding spaces are ignored.");
        answer.addValueChangeListener(e -> answer.setInvalid(false));

        password.setRequiredIndicatorVisible(true);
        password.setHelperText(PasswordPolicy.describe());
        password.addValueChangeListener(e -> password.setInvalid(false));

        confirmation.setRequiredIndicatorVisible(true);
        confirmation.addValueChangeListener(e -> confirmation.setInvalid(false));

        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.addClickListener(e -> submit());

        FormLayout form = new FormLayout(answer, password, confirmation, submit);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        challengeStep.add(questionLabel, form);
        challengeStep.setPadding(false);
        challengeStep.setVisible(false);
    }

    // The second step opens whether or not the account exists. An address the
    // system does not know still gets a question, so this form cannot be used
    // to find out which addresses are registered.
    private void loadChallenge() {
        if (email.isEmpty() || email.isInvalid()) {
            fail(email, "Enter the email address on your account");
            return;
        }

        RecoveryChallengeDto challenge = recoveryService.challengeFor(email.getValue());
        questionLabel.setText(questionText(challenge));
        challengeStep.setVisible(true);
        email.setReadOnly(true);
        findAccount.setVisible(false);
    }

    private String questionText(RecoveryChallengeDto challenge) {
        return switch (challenge.question()) {
            case FIRST_PET -> "What was the name of your first pet?";
            case BIRTH_CITY -> "Which city were you born in?";
            case PRIMARY_SCHOOL_TEACHER -> "What was your primary school teacher called?";
            case FAVOURITE_BOOK -> "What is your favourite book?";
        };
    }

    // No try-catch: a wrong answer or a locked account reaches the global
    // handler, which is the one place that turns an error code into a sentence.
    private void submit() {
        if (!validate()) {
            return;
        }

        recoveryService.recover(new PasswordRecoveryDto(
                email.getValue(), answer.getValue(), password.getValue()));

        Notification.show("Your password has been changed. Sign in with the new one.",
                        5000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        getUI().ifPresent(ui -> ui.navigate("login"));
    }

    private boolean validate() {
        boolean valid = true;

        if (answer.isEmpty()) {
            valid = fail(answer, "Answer the question");
        }

        if (password.isEmpty()) {
            valid = fail(password, "Choose a new password");
        } else if (!PasswordPolicy.isAcceptable(password.getValue())) {
            valid = fail(password, PasswordPolicy.describe());
        }

        if (confirmation.isEmpty()) {
            valid = fail(confirmation, "Type the password again");
        } else if (!password.getValue().equals(confirmation.getValue())) {
            valid = fail(confirmation, "The two passwords do not match");
        }

        return valid;
    }

    private boolean fail(HasValidation field, String message) {
        field.setErrorMessage(message);
        field.setInvalid(true);
        return false;
    }
}