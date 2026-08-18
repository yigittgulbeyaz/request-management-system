package com.yigit.requestms.common.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

// PermitAll rather than a role list: the layout itself carries no data, and
// restricting it would block every role whose views live inside it. Access is
// decided per view.
//
// Navigation items are filtered by role for usability only; a hidden link is
// never what keeps a user out.
@PermitAll
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
        if (authenticationContext.hasRole("PRODUCT_OWNER")) {
            nav.addItem(new SideNavItem("Prioritization Pool", "po/pool"));
        }
        if (authenticationContext.hasRole("DEVELOPER")) {
            nav.addItem(new SideNavItem("My Tasks", "dev/tasks"));
            nav.addItem(new SideNavItem("Available Tasks", "dev/available"));
        }
        if (authenticationContext.hasRole("ADMIN")) {
            nav.addItem(new SideNavItem("Users", "admin/users"));
        }
        return nav;
    }
}