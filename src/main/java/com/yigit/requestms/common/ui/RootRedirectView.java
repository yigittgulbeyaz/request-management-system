package com.yigit.requestms.common.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

// Never renders: beforeEnter forwards every visitor to the screen for their
// role. Div is here only because @Route requires a Component.
@Route("")
@PermitAll
public class RootRedirectView extends Div implements BeforeEnterObserver {

    private final AuthenticationContext authenticationContext;

    public RootRedirectView(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (authenticationContext.hasRole("CUSTOMER")) {
            event.forwardTo("requests/my");
        } else if (authenticationContext.hasRole("PRODUCT_OWNER")) {
            event.forwardTo("po/pool");
        } else if (authenticationContext.hasRole("DEVELOPER")) {
            event.forwardTo("dev/tasks");
        } else if (authenticationContext.hasRole("ADMIN")) {
            event.forwardTo("admin/users");
        }
    }
}
