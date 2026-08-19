package com.yigit.requestms.request.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.yigit.requestms.request.dto.CustomerRequestDetailDto;
import com.yigit.requestms.request.dto.StatusTimelineEntryDto;
import com.yigit.requestms.request.enums.RequestStatus;

import java.time.format.DateTimeFormatter;
import java.util.List;

// Shows one request in full, including the description that list projections
// leave out. Carries no score and no workflow stage: those belong to the
// product owner's and the developer's view of the same request.
public class RequestDetailDialog extends Dialog {

    private static final DateTimeFormatter TIMELINE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public RequestDetailDialog(CustomerRequestDetailDto request,
                               List<StatusTimelineEntryDto> timeline) {
        setHeaderTitle(request.title());
        setWidth("640px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.getStyle().set("max-height", "70vh").set("overflow-y", "auto");
        content.add(CustomerStatusPresentation.badge(request.status()));
        content.add(description(request.description()));

        if (request.status() == RequestStatus.REJECTED && request.rejectionReason() != null) {
            content.add(rejectionSection(request.rejectionReason()));
        }

        content.add(timelineSection(timeline));

        add(content);
        getFooter().add(new Button("Close", e -> close()));
    }

    private VerticalLayout rejectionSection(String reason) {
        H3 heading = new H3("Reason");
        heading.getStyle().set("font-size", "1rem").set("margin-bottom", "0.2em");

        VerticalLayout section = new VerticalLayout(heading, new Paragraph(reason));
        section.setPadding(false);
        section.setSpacing(false);
        return section;
    }

    // Scrolls within its own box rather than pushing the timeline off the
    // bottom of the dialog. A description can run to pages, and what happened
    // to the request should not depend on how much someone wrote.
    private Paragraph description(String text) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.getStyle()
                .set("max-height", "220px")
                .set("overflow-y", "auto")
                .set("white-space", "pre-wrap");
        return paragraph;
    }

    // Read forwards, oldest first, because it is the story of what happened
    // rather than a log to scan for the latest entry.
    //
    // Only the request's own states appear. The same trail holds the workflow
    // stages, and showing a customer that their request reached TESTING would
    // tell them something about how the work is going that they cannot act on.
    private VerticalLayout timelineSection(List<StatusTimelineEntryDto> timeline) {
        H3 heading = new H3("Progress");
        heading.getStyle().set("font-size", "1rem").set("margin-bottom", "0.2em");

        VerticalLayout section = new VerticalLayout(heading);
        section.setPadding(false);
        section.setSpacing(false);

        if (timeline.isEmpty()) {
            // Requests raised before the trail was recorded have nothing to
            // show, which is worth saying rather than leaving a blank heading.
            Paragraph empty = new Paragraph("No progress recorded for this request.");
            empty.getStyle().set("color", "#4a5568");
            section.add(empty);
            return section;
        }

        timeline.forEach(entry -> section.add(timelineRow(entry)));
        return section;
    }

    private HorizontalLayout timelineRow(StatusTimelineEntryDto entry) {
        Span when = new Span(entry.changedAt().format(TIMELINE_FORMAT));
        when.getStyle()
                .set("color", "#4a5568")
                .set("font-variant-numeric", "tabular-nums")
                .set("min-width", "130px");

        Span what = new Span(CustomerStatusPresentation.timelineLabel(entry.newStatus()));

        HorizontalLayout row = new HorizontalLayout(when, what);
        row.setSpacing(true);
        row.getStyle().set("padding", "0.25em 0");
        return row;
    }

}