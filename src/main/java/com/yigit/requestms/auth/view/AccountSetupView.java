package com.yigit.requestms.auth.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.yigit.requestms.auth.dto.AccountSetupDto;
import com.yigit.requestms.auth.service.AccountSetupService;
import com.yigit.requestms.auth.service.PasswordPolicy;
import com.yigit.requestms.user.enums.SecurityQuestion;

@Route("setup")
@PageTitle("Set up your account")
@AnonymousAllowed
public class AccountSetupView extends VerticalLayout {

    private final AccountSetupService accountSetupService;

    private final TextField setupCode = new TextField("Setup code");
    private final PasswordField password = new PasswordField("Password");
    private final PasswordField confirmation = new PasswordField("Confirm password");
    private final Select<SecurityQuestion> question = new Select<>();
    private final TextField answer = new TextField("Your answer");
    private final Button submit = new Button("Set up account");

    public AccountSetupView(AccountSetupService accountSetupService) {
        this.accountSetupService = accountSetupService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        configureFields();

        FormLayout form = new FormLayout(setupCode, password, confirmation, question, answer);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.setMaxWidth("440px");

        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.setWidthFull();
        submit.addClickListener(e -> submit());

        VerticalLayout card = new VerticalLayout(
                new H2("Set up your account"),
                new Paragraph("Use the code you were given. It works once, and "
                        + "what you choose here is what signs you in from now on."),
                form,
                submit);
        card.setMaxWidth("480px");

        add(card);
    }

    private void configureFields() {
        setupCode.setRequiredIndicatorVisible(true);
        setupCode.setPlaceholder("XXXX-XXXX-XXXX");
        setupCode.setHelperText("Dashes and capitals are optional.");
        setupCode.addValueChangeListener(e -> setupCode.setInvalid(false));

        password.setRequiredIndicatorVisible(true);
        password.setHelperText(PasswordPolicy.describe());
        password.addValueChangeListener(e -> password.setInvalid(false));

        confirmation.setRequiredIndicatorVisible(true);
        confirmation.addValueChangeListener(e -> confirmation.setInvalid(false));

        // A fixed list rather than free text: a question someone writes
        // themselves tends to have an answer their colleagues know, and a
        // stored sentence cannot be translated the way a key can.
        question.setLabel("Security question");
        question.setItems(SecurityQuestion.values());
        question.setItemLabelGenerator(SecurityQuestion::name);
        question.setRequiredIndicatorVisible(true);
        question.addValueChangeListener(e -> question.setInvalid(false));

        answer.setRequiredIndicatorVisible(true);
        answer.setHelperText("Used if you ever forget your password. "
                + "Case and surrounding spaces are ignored.");
        answer.addValueChangeListener(e -> answer.setInvalid(false));
    }

    // The button stays enabled and the form answers on submit. A disabled
    // button with no explanation leaves someone guessing which field it is
    // waiting on, and the guess is usually wrong.
    private void submit() {
        if (!validate()) {
            return;
        }

        accountSetupService.complete(new AccountSetupDto(
                setupCode.getValue(), password.getValue(),
                question.getValue(), answer.getValue()));

        Notification.show("Your account is ready. Sign in with your new password.",
                        5000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        getUI().ifPresent(ui -> ui.navigate("login"));
    }

    // Every field is checked rather than stopping at the first, so someone who
    // got two things wrong finds out about both at once.
    private boolean validate() {
        boolean valid = true;

        if (setupCode.isEmpty()) {
            valid = fail(setupCode, "Enter the code you were given");
        }

        if (password.isEmpty()) {
            valid = fail(password, "Choose a password");
        } else if (!PasswordPolicy.isAcceptable(password.getValue())) {
            valid = fail(password, PasswordPolicy.describe());
        }

        if (confirmation.isEmpty()) {
            valid = fail(confirmation, "Type the password again");
        } else if (!password.getValue().equals(confirmation.getValue())) {
            valid = fail(confirmation, "The two passwords do not match");
        }

        if (question.getValue() == null) {
            valid = fail(question, "Pick a question");
        }

        if (answer.isEmpty()) {
            valid = fail(answer, "Answer the question");
        }

        return valid;
    }

    private boolean fail(com.vaadin.flow.component.HasValidation field, String message) {
        field.setErrorMessage(message);
        field.setInvalid(true);
        return false;
    }
}