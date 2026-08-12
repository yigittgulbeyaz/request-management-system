package com.yigit.requestms.prioritization.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.yigit.requestms.common.ui.MainLayout;
import com.yigit.requestms.common.ui.StatusBadge;
import com.yigit.requestms.prioritization.dto.PrioritizationDetailDto;
import com.yigit.requestms.prioritization.dto.PrioritizationFormDto;
import com.yigit.requestms.prioritization.enums.ImpactLevel;
import com.yigit.requestms.prioritization.enums.PriorityBand;
import com.yigit.requestms.prioritization.enums.UrgencyLevel;
import com.yigit.requestms.prioritization.service.PrioritizationService;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "po/prioritize", layout = MainLayout.class)
@PageTitle("Prioritize Request")
@RolesAllowed("PRODUCT_OWNER")
public class PrioritizationFormView extends VerticalLayout implements BeforeEnterObserver {

    private final PrioritizationService prioritizationService;

    private final Select<ImpactLevel> impact = new Select<>();
    private final Select<UrgencyLevel> urgency = new Select<>();

    private final H1 scoreValue = new H1("-");
    private final Span scoreFormula = new Span();
    private final Div scoreBandSlot = new Div();
    private final Button save = new Button("Save Values");

    private final H2 heading = new H2();
    private final Paragraph customerLine = new Paragraph();
    private final Paragraph descriptionLine = new Paragraph();

    private Long requestId;

    public PrioritizationFormView(PrioritizationService prioritizationService) {
        this.prioritizationService = prioritizationService;

        setMaxWidth("1000px");
        configureSelects();

        add(heading, customerLine, descriptionLine,
                new HorizontalLayout(buildInputs(), buildScoreCard()));
    }

    // The request id travels as a query parameter rather than a field on the
    // view: a view instance is reused across navigations, so reading it on
    // entry is what keeps the screen showing the request that was asked for.
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Long id = readRequestId(event);

        // Entry is refused rather than the save: a request that can no longer
        // be scored should not open a form whose submission would fail. The
        // global handler can report but not redirect, so this decision is local.
        if (id == null || !prioritizationService.isScorable(id)) {
            event.forwardTo("po/pool");
            return;
        }

        requestId = id;
        render(prioritizationService.loadForScoring(id));
    }

    private Long readRequestId(BeforeEnterEvent event) {
        return event.getLocation().getQueryParameters()
                .getSingleParameter("requestId")
                .filter(raw -> raw.matches("\\d+"))
                .map(Long::valueOf)
                .orElse(null);
    }

    private void configureSelects() {
        impact.setLabel("Business Impact");
        impact.setItems(ImpactLevel.values());
        impact.setItemLabelGenerator(level -> level.getValue() + " - " + level.getLabel());
        impact.setWidthFull();
        impact.addValueChangeListener(e -> refreshScore());

        urgency.setLabel("Urgency");
        urgency.setItems(UrgencyLevel.values());
        urgency.setItemLabelGenerator(level -> level.getValue() + " - " + level.getLabel());
        urgency.setWidthFull();
        urgency.addValueChangeListener(e -> refreshScore());
    }

    private VerticalLayout buildInputs() {
        VerticalLayout inputs = new VerticalLayout(impact, urgency);
        inputs.setPadding(false);
        inputs.setWidth("60%");
        return inputs;
    }

    private VerticalLayout buildScoreCard() {
        Span caption = new Span("CALCULATED SCORE");
        caption.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);

        scoreValue.addClassNames(LumoUtility.Margin.NONE);
        scoreFormula.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.setEnabled(false);
        save.setWidthFull();
        save.addClickListener(e -> save());

        VerticalLayout card = new VerticalLayout(
                caption, scoreValue, scoreFormula, scoreBandSlot, save);
        card.setAlignItems(Alignment.CENTER);
        card.setWidth("40%");
        card.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BorderColor.CONTRAST_10);
        return card;
    }

    private void render(PrioritizationDetailDto detail) {
        heading.setText("Request evaluation - #" + detail.requestId()
                + " " + detail.requestTitle());
        customerLine.setText("Customer: " + detail.customerName());
        descriptionLine.setText(detail.description());

        if (detail.isScored()) {
            impact.setValue(detail.impact());
            urgency.setValue(detail.urgency());
        } else {
            impact.clear();
            urgency.clear();
        }
        refreshScore();
    }

    // Preview only. The stored score comes from the database, which derives it
    // from the same two inputs; this figure is never sent back.
    private void refreshScore() {
        ImpactLevel selectedImpact = impact.getValue();
        UrgencyLevel selectedUrgency = urgency.getValue();

        scoreBandSlot.removeAll();

        if (selectedImpact == null || selectedUrgency == null) {
            scoreValue.setText("-");
            scoreFormula.setText("Select both values");
            save.setEnabled(false);
            return;
        }

        int score = selectedImpact.getValue() * selectedUrgency.getValue();
        PriorityBand band = PriorityBand.ofScore(score);

        scoreValue.setText(String.valueOf(score));
        scoreFormula.setText(selectedImpact.getValue() + " (Impact) x "
                + selectedUrgency.getValue() + " (Urgency)");
        scoreBandSlot.add(new StatusBadge(band.getLabel(), bandTone(band)));
        save.setEnabled(true);
    }

    private StatusBadge.Tone bandTone(PriorityBand band) {
        return switch (band) {
            case LOW -> StatusBadge.Tone.POSITIVE;
            case MEDIUM -> StatusBadge.Tone.WARNING;
            case CRITICAL -> StatusBadge.Tone.NEGATIVE;
        };
    }

    // No try-catch: a rule broken in the service reaches the global error
    // handler, which is the one place that knows how to say so.
    private void save() {
        prioritizationService.score(requestId,
                new PrioritizationFormDto(impact.getValue(), urgency.getValue()));

        Notification.show("Score saved.", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        getUI().ifPresent(ui -> ui.navigate("po/pool"));
    }
}