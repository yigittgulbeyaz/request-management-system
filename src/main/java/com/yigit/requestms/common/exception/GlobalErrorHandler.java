package com.yigit.requestms.common.exception;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Views throw rather than catch. A rule broken in the service layer is not
// something a screen can do anything about beyond telling the user, and having
// every screen write that telling is the repetition this replaces.
//
// A BaseException carries an error code, so it maps to a sentence written for
// the user. Anything else is a defect: the user gets a neutral message and the
// detail goes to the log, where it is of use to someone who can act on it.
public class GlobalErrorHandler implements ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    @Override
    public void error(ErrorEvent event) {
        Throwable cause = unwrap(event.getThrowable());

        if (cause instanceof BaseException known) {
            log.debug("Rule violation surfaced to the user: {}", known.getMessage());
            show(ErrorMessages.forCode(known.getErrorCode()));
            return;
        }

        log.error("Unhandled failure", cause);
        show(ErrorMessages.forCode(null));
    }

    // Vaadin wraps what a listener threw, sometimes more than once. Package
    // private and static so the unwrapping can be tested on its own: the rest
    // of this class needs a live UI, and what matters here is finding the rule
    // that was broken inside whatever it arrived in.
    static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && !(current instanceof BaseException)) {
            current = current.getCause();
        }
        return current;
    }

    // The error arrives on the session thread, so the UI has to be told to
    // update itself rather than being written to directly.
    private void show(String message) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return;
        }
        ui.access(() -> Notification
                .show(message, 5000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR));
    }
}