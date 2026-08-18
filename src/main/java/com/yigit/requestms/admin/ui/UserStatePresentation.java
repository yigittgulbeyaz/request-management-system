package com.yigit.requestms.admin.ui;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.yigit.requestms.admin.dto.AdminUserDto;
import com.yigit.requestms.common.ui.StatusBadge;
import com.yigit.requestms.user.enums.Role;

// An account can be inactive, locked, awaiting a first sign-in, or none of
// those, and the states are independent: locked is what the system did after
// failed reset attempts, inactive is what an administrator decided. Showing
// them as separate badges rather than one status keeps them from being read as
// alternatives.
public final class UserStatePresentation {

    private UserStatePresentation() {
    }

    public static HorizontalLayout stateBadges(AdminUserDto user) {
        HorizontalLayout badges = new HorizontalLayout();
        badges.setSpacing(false);
        badges.getStyle().set("gap", "0.35em");

        if (!user.active()) {
            badges.add(new StatusBadge("Inactive", StatusBadge.Tone.NEUTRAL));
        }
        if (user.locked()) {
            badges.add(new StatusBadge("Locked", StatusBadge.Tone.NEGATIVE));
        }
        if (user.mustChangePassword()) {
            badges.add(new StatusBadge("Temp password", StatusBadge.Tone.WARNING));
        }
        if (badges.getComponentCount() == 0) {
            badges.add(new StatusBadge("Active", StatusBadge.Tone.POSITIVE));
        }
        return badges;
    }

    public static StatusBadge roleBadge(Role role) {
        return new StatusBadge(label(role), tone(role));
    }

    public static String label(Role role) {
        return switch (role) {
            case CUSTOMER -> "Customer";
            case PRODUCT_OWNER -> "Product Owner";
            case DEVELOPER -> "Developer";
            case ADMIN -> "Admin";
        };
    }

    private static StatusBadge.Tone tone(Role role) {
        return switch (role) {
            case CUSTOMER -> StatusBadge.Tone.NEUTRAL;
            case PRODUCT_OWNER -> StatusBadge.Tone.ACTIVE;
            case DEVELOPER -> StatusBadge.Tone.POSITIVE;
            case ADMIN -> StatusBadge.Tone.WARNING;
        };
    }
}