package com.yigit.requestms.common.ui;

import com.vaadin.flow.component.html.Span;

// Shared across every screen that displays a status. The label is passed in
// rather than derived here because the same status reads differently per role:
// a customer sees "In progress" where a product owner sees IN_WORKFLOW.
//
// Literal colours rather than Lumo's custom properties: the variables resolve
// against a stylesheet this application does not import, so they came out
// blank. Fixed values are duller than theming but they are visible.
public class StatusBadge extends Span {

    public enum Tone {
        NEUTRAL("#4a5568", "#edf2f7"),
        ACTIVE("#1a56b8", "#e3f0ff"),
        POSITIVE("#1a7a3c", "#e4f5ea"),
        WARNING("#8a6100", "#fff4d6"),
        NEGATIVE("#a52020", "#fdeaea");

        private final String textColour;
        private final String background;

        Tone(String textColour, String background) {
            this.textColour = textColour;
            this.background = background;
        }
    }

    public StatusBadge(String label, Tone tone) {
        super(label);
        getStyle()
                .set("color", tone.textColour)
                .set("background-color", tone.background)
                .set("border-radius", "4px")
                .set("padding", "0.15em 0.6em")
                .set("font-size", "0.875rem")
                .set("font-weight", "500")
                .set("white-space", "nowrap")
                .set("display", "inline-block");
    }
}