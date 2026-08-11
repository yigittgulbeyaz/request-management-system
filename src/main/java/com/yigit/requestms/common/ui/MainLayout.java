package com.yigit.requestms.common.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.theme.lumo.LumoUtility;

// Parent layout for every authenticated view. Navigation items are filtered by
// role for usability only; access itself is enforced by @RolesAllowed on each
// view, so hiding a link is never the thing that keeps a user out.
public class MainLayout extends AppLayout {

    private final AuthenticationContext authenticationContext;

    public MainLayout(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
        addToNavbar(buildHeader());
        addToDrawer(buildNavigation());
    }

    private HorizontalLayout buildHeader() {
        H2 title = new H2("Request Management System");
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        Button logout = new Button("Sign out", e -> authenticationContext.logout());
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), title, logout);
        header.setWidthFull();
        header.expand(title);
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        header.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM);
        return header;
    }

    private SideNav buildNavigation() {
        SideNav nav = new SideNav();
        if (authenticationContext.hasRole("CUSTOMER")) {
            nav.addItem(new SideNavItem("New Request", "requests/new"));
            nav.addItem(new SideNavItem("My Requests", "requests/my"));
        }
        return nav;
    }
}