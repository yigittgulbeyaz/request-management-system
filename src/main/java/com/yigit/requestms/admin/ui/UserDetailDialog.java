package com.yigit.requestms.admin.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.yigit.requestms.admin.dto.UserDetailDto;
import com.yigit.requestms.common.ui.StatusBadge;

import java.time.format.DateTimeFormatter;

// One account in full. The account details are here now; the record of what
// this person has been doing arrives with the reporting views, which is where
// the queries behind it belong.
//
// Deliberately read-only. Everything that changes an account is already in the
// row's action menu, and offering the same thing twice invites the two to
// disagree about what confirmation each needs.
public class UserDetailDialog extends Dialog {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public UserDetailDialog(UserDetailDto user) {
        setHeaderTitle(user.nameSurname());
        setWidth("620px");

        VerticalLayout content = new VerticalLayout(
                accountSection(user),
                securitySection(user),
                activityPlaceholder());
        content.setPadding(false);

        add(content);
        getFooter().add(new Button("Close", e -> close()));
    }

    private VerticalLayout accountSection(UserDetailDto user) {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        form.addFormItem(new Span(user.email()), "Email");
        form.addFormItem(UserStatePresentation.roleBadge(user.role()), "Role");
        form.addFormItem(stateBadges(user), "State");
        form.addFormItem(new Span(user.createdAt().format(DATE_FORMAT)), "Joined");

        return section("Account", form);
    }

    private HorizontalLayout stateBadges(UserDetailDto user) {
        HorizontalLayout badges = new HorizontalLayout();
        badges.setSpacing(false);
        badges.getStyle().set("gap", "0.35em");

        if (!user.active()) {
            badges.add(new StatusBadge("Inactive", StatusBadge.Tone.NEUTRAL));
        }
        if (user.locked()) {
            badges.add(new StatusBadge("Locked", StatusBadge.Tone.NEGATIVE));
        }
        if (user.awaitingSetup()) {
            badges.add(new StatusBadge("Awaiting setup", StatusBadge.Tone.WARNING));
        }
        if (badges.getComponentCount() == 0) {
            badges.add(new StatusBadge("Active", StatusBadge.Tone.POSITIVE));
        }
        return badges;
    }

    private VerticalLayout securitySection(UserDetailDto user) {
        if (user.awaitingSetup()) {
            // Nothing to show yet: the question is chosen during setup, and an
            // account waiting for that has no answer to compare against.
            Paragraph note = new Paragraph(
                    "The security question is chosen by the account holder when "
                            + "they use their setup code.");
            note.getStyle().set("color", "#4a5568");
            return section("Security", note);
        }

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        // The question is named, the answer is not shown and cannot be: only
        // its hash is stored, and an administrator able to read it would make
        // the question worthless as a way of proving who someone is.
        form.addFormItem(new Span(user.securityQuestion().name()), "Security question");
        form.addFormItem(new Span(String.valueOf(user.failedResetAttempts())),
                "Failed reset attempts");

        return section("Security", form);
    }

    private VerticalLayout activityPlaceholder() {
        Paragraph note = new Paragraph(
                "Completed work, average turnaround and deadline record appear "
                        + "here once the reporting views are in place.");
        note.getStyle().set("color", "#4a5568");

        return section("Activity", note);
    }

    private VerticalLayout section(String title, Component body) {
        H3 heading = new H3(title);
        heading.getStyle().set("font-size", "1rem").set("margin-bottom", "0.2em");

        VerticalLayout section = new VerticalLayout(heading, body);
        section.setPadding(false);
        section.setSpacing(false);
        return section;
    }
}