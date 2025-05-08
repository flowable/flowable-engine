package org.flowable.common.rest.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class DateFormattingTest {

    @Test
    public void testValidDates() {
        assertEquals(
                Date.from(Instant.parse("2024-05-08T14:30:00Z")),
                RequestUtil.parseLongDate("2024-05-08T14:30Z")
        );

        assertEquals(
                Date.from(Instant.parse("2024-05-08T14:30:45Z")),
                RequestUtil.parseLongDate("2024-05-08T14:30:45Z")
        );

        assertEquals(
                Date.from(Instant.parse("2024-05-08T14:30:45.123Z")),
                RequestUtil.parseLongDate("2024-05-08T14:30:45.123Z")
        );

        assertEquals(
                Date.from(Instant.parse("2024-05-08T12:30:00Z")),
                RequestUtil.parseLongDate("2024-05-08T14:30+02:00")
        );

        assertEquals(
                Date.from(Instant.parse("2024-05-08T12:30:45Z")),
                RequestUtil.parseLongDate("2024-05-08T14:30:45+02:00")
        );

        assertEquals(
                Date.from(Instant.parse("2024-05-08T12:30:45.123Z")),
                RequestUtil.parseLongDate("2024-05-08T14:30:45.123+02:00")
        );
    }

    @Test
    public void testInvalidDates() {
        assertThrows(Exception.class, () -> RequestUtil.parseLongDate("2024-05-08"));
        assertThrows(Exception.class, () -> RequestUtil.parseLongDate("2024-05-08T14"));
        assertThrows(Exception.class, () -> RequestUtil.parseLongDate("2024-05-08T:30"));
        assertThrows(Exception.class, () -> RequestUtil.parseLongDate("May 8, 2024"));
        assertThrows(Exception.class, () -> RequestUtil.parseLongDate("2024-05-08T14-30"));
        assertThrows(NullPointerException.class, () -> RequestUtil.parseLongDate(null));
    }
}
