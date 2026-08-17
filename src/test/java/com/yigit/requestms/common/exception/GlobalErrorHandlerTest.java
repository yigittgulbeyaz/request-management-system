package com.yigit.requestms.common.exception;

import com.yigit.requestms.request.exception.RequestNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Vaadin hands the handler whatever the listener threw, wrapped in its own
// exception and sometimes wrapped again by Spring. A rule the user broke has
// to be found inside that, or it reaches them as "something went wrong" and
// looks like a defect.
class GlobalErrorHandlerTest {

    @Test
    @DisplayName("finds the rule that was broken inside two layers of wrapping")
    void findsTheCauseInsideWrappers() {
        BaseException rule = new RequestNotFoundException(42L);
        Throwable wrapped = new RuntimeException("outer",
                new IllegalStateException("inner", rule));

        assertThat(GlobalErrorHandler.unwrap(wrapped)).isSameAs(rule);
    }

    @Test
    @DisplayName("returns the exception itself when nothing wraps it")
    void unwrappedStaysUnwrapped() {
        BaseException rule = new RequestNotFoundException(42L);

        assertThat(GlobalErrorHandler.unwrap(rule)).isSameAs(rule);
    }

    @Test
    @DisplayName("stops at the first rule rather than digging past it")
    void stopsAtTheOutermostRule() {
        BaseException outer = new RequestNotFoundException(1L);
        BaseException inner = new RequestNotFoundException(2L);
        // Contrived, but it fixes the direction: the rule nearest the surface
        // is the one the user's action broke.
        Throwable chain = new RuntimeException("wrapper", outer);
        outer.initCause(inner);

        assertThat(GlobalErrorHandler.unwrap(chain)).isSameAs(outer);
    }

    @Test
    @DisplayName("returns the deepest cause when none of them is a rule")
    void defectsUnwrapToTheirRoot() {
        Throwable root = new NullPointerException("the actual defect");
        Throwable wrapped = new RuntimeException("outer", new IllegalStateException("inner", root));

        // Nothing here maps to a sentence, so the user gets the fallback while
        // the log gets this.
        assertThat(GlobalErrorHandler.unwrap(wrapped)).isSameAs(root);
    }

    @Test
    @DisplayName("a lone defect comes back as itself")
    void loneDefectComesBackAsItself() {
        Throwable defect = new NullPointerException("nothing wrapped it");

        assertThat(GlobalErrorHandler.unwrap(defect)).isSameAs(defect);
    }
}