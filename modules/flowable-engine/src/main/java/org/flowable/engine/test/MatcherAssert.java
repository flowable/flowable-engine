package org.flowable.engine.test;

import org.flowable.engine.delegate.BpmnError;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

/**
 * This class evaluates assertions and throws {@link BpmnError} in the case of failure
 */
public class MatcherAssert {
    public MatcherAssert() {
    }

    public static <T> void assertThat(T actual, Matcher<? super T> matcher) {
        assertThat("", actual, matcher);
    }

    public static <T> void assertThat(String reason, T actual, Matcher<? super T> matcher) {
        if (!matcher.matches(actual)) {
            Description description = new StringDescription();
            description.appendText(reason).appendText("\nExpected: ").appendDescriptionOf(matcher).appendText("\n     but: ");
            matcher.describeMismatch(actual, description);
            throw new BpmnError(description.toString());
        }
    }

    public static void assertThat(String reason, boolean assertion) {
        if (!assertion) {
            throw new AssertionError(reason);
        }
    }
}
