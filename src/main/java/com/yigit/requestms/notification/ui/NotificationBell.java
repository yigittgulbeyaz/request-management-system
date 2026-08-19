package com.yigit.requestms.notification.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.yigit.requestms.notification.dto.NotificationDto;
import com.yigit.requestms.notification.service.NotificationService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

// Sits in the navbar for every role. The count is read when the layout is
// built and again whenever the menu is opened, rather than pushed from the
// server: a notice that arrives while someone is reading a grid can wait until
// they look.
public class NotificationBell extends HorizontalLayout {

    private final NotificationService notificationService;

    private final Button trigger = new Button(new Icon(VaadinIcon.BELL_O));
    private final Span badge = new Span();
    private final VerticalLayout list = new VerticalLayout();
    private final Popover popover = new Popover();

    public NotificationBell(NotificationService notificationService) {
        this.notificationService = notificationService;

        configureTrigger();
        configurePopover();

        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        add(trigger, badge);

        refreshBadge();
    }

    private void configureTrigger() {
        trigger.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        trigger.getElement().setAttribute("aria-label", "Notifications");
    }

    private void configurePopover() {
        popover.setTarget(trigger);
        popover.setPosition(PopoverPosition.BOTTOM_END);
        popover.setWidth("380px");
        popover.addOpenedChangeListener(e -> {
            if (e.isOpened()) {
                refreshList();
            }
        });
        popover.add(header(), list);

        list.setPadding(false);
        list.setSpacing(false);
        list.getStyle().set("max-height", "380px").set("overflow-y", "auto");
    }

    private HorizontalLayout header() {
        Span title = new Span("Notifications");
        title.getStyle().set("font-weight", "600");

        Button markRead = new Button("Mark all read", e -> {
            notificationService.markAllRead();
            refreshList();
            refreshBadge();
        });
        markRead.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);

        HorizontalLayout header = new HorizontalLayout(title, markRead);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.expand(title);
        header.getStyle().set("padding", "0.4em 0.2em");
        return header;
    }

    private void refreshBadge() {
        long unread = notificationService.unreadCount();

        badge.setVisible(unread > 0);
        badge.setText(unread > 9 ? "9+" : String.valueOf(unread));
        badge.getStyle()
                .set("background-color", "#a52020")
                .set("color", "white")
                .set("border-radius", "999px")
                .set("font-size", "0.7rem")
                .set("font-weight", "600")
                .set("padding", "0.05em 0.45em")
                .set("margin-left", "-0.6em")
                .set("margin-top", "-0.8em");
    }

    private void refreshList() {
        list.removeAll();
        List<NotificationDto> notices = notificationService.recent();

        if (notices.isEmpty()) {
            Paragraph empty = new Paragraph("Nothing yet.");
            empty.getStyle().set("color", "#4a5568").set("padding", "0.6em 0.2em");
            list.add(empty);
            return;
        }

        notices.forEach(notice -> list.add(row(notice)));
    }

    // Unread notices are marked rather than coloured differently: the point is
    // which ones are new, and a dot says that without a second colour scheme.
    private VerticalLayout row(NotificationDto notice) {
        Span message = new Span((notice.read() ? "" : "• ") + notice.message());
        if (!notice.read()) {
            message.getStyle().set("font-weight", "500");
        }

        Span when = new Span(relative(notice.createdAt()));
        when.getStyle().set("color", "#4a5568").set("font-size", "0.8rem");

        VerticalLayout row = new VerticalLayout(message, when);
        row.setPadding(false);
        row.setSpacing(false);
        row.getStyle()
                .set("padding", "0.5em 0.2em")
                .set("border-bottom", "1px solid #edf2f7");

        // Clicking through only makes sense when the notice is about something.
        if (notice.relatedRequestId() != null) {
            row.getStyle().set("cursor", "pointer");
            row.getElement().addEventListener("click", e -> openRelated());
        }
        return row;
    }

    // Sends the reader to their own list rather than to the request directly:
    // which screen shows a given request depends on the role, and the list is
    // the one page every role has.
    private void openRelated() {
        popover.close();
        UI.getCurrent().navigate("");
    }

    // Relative rather than a timestamp. "2 hours ago" answers the question a
    // notice raises, where a date makes the reader work out the difference.
    private String relative(LocalDateTime when) {
        Duration elapsed = Duration.between(when, LocalDateTime.now());

        if (elapsed.toMinutes() < 1) {
            return "just now";
        }
        if (elapsed.toHours() < 1) {
            return elapsed.toMinutes() + "m ago";
        }
        if (elapsed.toDays() < 1) {
            return elapsed.toHours() + "h ago";
        }
        return elapsed.toDays() + "d ago";
    }
}