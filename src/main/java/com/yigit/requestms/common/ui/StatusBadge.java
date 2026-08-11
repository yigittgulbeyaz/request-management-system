package com.yigit.requestms.common.ui;

import com.vaadin.flow.component.html.Span;

// Shared across every screen that displays a status. The label is passed in
// rather than derived here because the same status reads differently per role:
// a customer sees "In progress" where a product owner sees IN_WORKFLOW.
public class StatusBadge extends Span {

    public enum Tone {
        NEUTRAL("contrast"),
        ACTIVE("primary"),
        POSITIVE("success"),
        NEGATIVE("error"),
        WARNING("warning");

        private final String themeName;

        Tone(String themeName) {
            this.themeName = themeName;
        }
    }

    public StatusBadge(String label, Tone tone) {
        super(label);
        getElement().getThemeList().add("badge");
        getElement().getThemeList().add(tone.themeName);
    }
}