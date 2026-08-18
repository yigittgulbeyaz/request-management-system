package com.yigit.requestms.auth.view;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Sign in")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        loginForm.setAction("login");
        // Vaadin's own forgot-password button raises an event rather than
        // navigating, so it would need a listener to do what the link below
        // already does. Two of them on one screen is one too many.
        loginForm.setForgotPasswordButtonVisible(false);
        loginForm.setI18n(buildI18n());

        add(new H1("Request Management System"),
                loginForm,
                new Anchor("recover", "Forgotten your password?"),
                new Anchor("setup", "Have a setup code?"));
    }

    // Wrong password, unknown account and locked account all produce the same
    // message: distinguishing them would let an attacker enumerate valid
    // accounts and discover which ones are locked.
    private LoginI18n buildI18n() {
        LoginI18n i18n = LoginI18n.createDefault();
        i18n.getForm().setTitle("Sign in");
        i18n.getForm().setUsername("Email");
        i18n.getForm().setPassword("Password");
        i18n.getForm().setSubmit("Sign in");
        i18n.getErrorMessage().setTitle("Sign-in failed");
        i18n.getErrorMessage().setMessage(
                "Invalid credentials, or this account is unavailable.");
        return i18n;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            loginForm.setError(true);
        }
    }
}