package com.yigit.requestms.admin.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.yigit.requestms.admin.dto.AdminUserDto;
import com.yigit.requestms.admin.dto.CreatedUserDto;
import com.yigit.requestms.admin.service.AdminUserService;
import com.yigit.requestms.admin.ui.CreateUserDialog;
import com.yigit.requestms.admin.ui.TemporaryPasswordDialog;
import com.yigit.requestms.admin.ui.UserDetailDialog;
import com.yigit.requestms.admin.ui.UserStatePresentation;
import com.yigit.requestms.common.ui.MainLayout;
import com.yigit.requestms.user.enums.Role;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;

@Route(value = "admin/users", layout = MainLayout.class)
@PageTitle("Users")
@RolesAllowed("ADMIN")
public class UserManagementView extends VerticalLayout {

    private static final String ALL_ROLES_LABEL = "All roles";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final AdminUserService adminUserService;

    private final Grid<AdminUserDto> grid = new Grid<>();
    private final TextField search = new TextField();
    private final Select<Role> roleFilter = new Select<>();

    public UserManagementView(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;

        setSizeFull();
        configureFilters();
        configureGrid();

        add(header(), new HorizontalLayout(search, roleFilter), grid);
    }

    private HorizontalLayout header() {
        Button create = new Button("New User", new Icon(VaadinIcon.PLUS),
                e -> openCreateDialog());
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout header = new HorizontalLayout(new H2("Users"), create);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.expand(header.getComponentAt(0));
        return header;
    }

    private void configureFilters() {
        search.setPlaceholder("Search name or email");
        search.setClearButtonVisible(true);
        search.setWidth("280px");
        // LAZY rather than EAGER: a query per keystroke would ask the database
        // for results nobody has finished asking for.
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.addValueChangeListener(e -> grid.getDataProvider().refreshAll());

        roleFilter.setPlaceholder(ALL_ROLES_LABEL);
        roleFilter.setItems(Role.values());
        roleFilter.setItemLabelGenerator(role ->
                role == null ? ALL_ROLES_LABEL : UserStatePresentation.label(role));
        roleFilter.setEmptySelectionAllowed(true);
        roleFilter.setEmptySelectionCaption(ALL_ROLES_LABEL);
        roleFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
    }

    private void configureGrid() {
        grid.addComponentColumn(this::nameLink)
                .setHeader("Name")
                .setFlexGrow(1);

        grid.addColumn(AdminUserDto::email)
                .setHeader("Email")
                .setFlexGrow(1);

        grid.addComponentColumn(dto -> UserStatePresentation.roleBadge(dto.role()))
                .setHeader("Role")
                .setWidth("160px")
                .setFlexGrow(0);

        grid.addComponentColumn(UserStatePresentation::stateBadges)
                .setHeader("State")
                .setWidth("230px")
                .setFlexGrow(0);

        grid.addColumn(dto -> dto.createdAt().format(DATE_FORMAT))
                .setHeader("Joined")
                .setWidth("130px")
                .setFlexGrow(0);

        // An overflow menu rather than a row of buttons: most of these actions
        // are rare, and giving each its own button would make the common ones
        // harder to find.
        grid.addComponentColumn(this::actionsMenu)
                .setHeader("Actions")
                .setWidth("90px")
                .setFlexGrow(0);

        grid.setSizeFull();
        grid.setEmptyStateText("No users match these filters.");

        grid.setItemsPageable(
                pageable -> adminUserService.list(roleFilter.getValue(), search.getValue(), pageable),
                query -> (int) adminUserService.count(roleFilter.getValue(), search.getValue()));
    }

    private MenuBar actionsMenu(AdminUserDto user) {
        MenuBar menu = new MenuBar();
        menu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        var root = menu.addItem(new Icon(VaadinIcon.ELLIPSIS_DOTS_H));
        var items = root.getSubMenu();

        addRoleItems(items, user);

        if (user.locked()) {
            items.addItem("Unlock and reset password", e -> confirmUnlock(user));
        }

        if (user.active()) {
            items.addItem("Deactivate", e -> confirmDeactivate(user));
        } else {
            items.addItem("Reactivate", e -> {
                adminUserService.reactivate(user.id());
                notifySuccess(user.nameSurname() + " can sign in again.");
                grid.getDataProvider().refreshAll();
            });
        }
        return menu;
    }

    private void addRoleItems(com.vaadin.flow.component.contextmenu.SubMenu items,
                              AdminUserDto user) {
        var roleItem = items.addItem("Change role");
        for (Role role : Role.values()) {
            if (role == user.role()) {
                continue;
            }
            roleItem.getSubMenu().addItem(UserStatePresentation.label(role),
                    e -> confirmRoleChange(user, role));
        }
    }

    // Role changes and deactivation take effect on the next request the person
    // makes, so they are confirmed: the person on the other end finds out by
    // losing access rather than by being told.
    private void confirmRoleChange(AdminUserDto user, Role newRole) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Change role?");
        dialog.setText(user.nameSurname() + " becomes a "
                + UserStatePresentation.label(newRole)
                + ". Their existing work keeps the name it already has.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Change role");
        dialog.addConfirmListener(e -> {
            adminUserService.changeRole(user.id(), newRole);
            notifySuccess(user.nameSurname() + " is now a "
                    + UserStatePresentation.label(newRole) + ".");
            grid.getDataProvider().refreshAll();
        });
        dialog.open();
    }

    private void confirmDeactivate(AdminUserDto user) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Deactivate account?");
        dialog.setText(user.nameSurname() + " will not be able to sign in. "
                + "Nothing they did is removed, and their name keeps appearing "
                + "on the work they did.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Deactivate");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            adminUserService.deactivate(user.id());
            notifySuccess(user.nameSurname() + " can no longer sign in.");
            grid.getDataProvider().refreshAll();
        });
        dialog.open();
    }

    private void confirmUnlock(AdminUserDto user) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Unlock and reset password?");
        dialog.setText("A new temporary password is issued and shown once. "
                + "The old one stops working immediately.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Unlock");
        dialog.addConfirmListener(e -> {
            CreatedUserDto result = adminUserService.unlockWithNewPassword(user.id());
            grid.getDataProvider().refreshAll();
            new TemporaryPasswordDialog(result).open();
        });
        dialog.open();
    }

    private void openCreateDialog() {
        new CreateUserDialog(form -> {
            CreatedUserDto created = adminUserService.create(form);
            grid.getDataProvider().refreshAll();
            new TemporaryPasswordDialog(created).open();
        }).open();
    }

    private void notifySuccess(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private Button nameLink(AdminUserDto user) {
        Button link = new Button(user.nameSurname(), e -> openDetail(user));
        link.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        link.getStyle().set("text-align", "left");
        return link;
    }

    private void openDetail(AdminUserDto user) {
        new UserDetailDialog(adminUserService.detail(user.id())).open();
    }
}