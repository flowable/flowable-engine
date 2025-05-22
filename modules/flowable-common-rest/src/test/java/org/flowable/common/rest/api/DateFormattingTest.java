/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.common.rest.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DateFormattingTest {

    @Test
    public void testValidDates() {
        assertThat(RequestUtil.parseLongDate("2024-05-08"))
                .isEqualTo(toDate("2024-05-08T00:00:00Z"));

        assertThat(RequestUtil.parseLongDate("2024-05-08T14:30"))
                .isEqualTo(toDate("2024-05-08T14:30:00Z"));

        assertThat(RequestUtil.parseLongDate("2024-05-08T14:30Z"))
                .isEqualTo(toDate("2024-05-08T14:30:00Z"));

        assertThat(RequestUtil.parseLongDate("2024-05-08T14:30:45Z"))
                .isEqualTo(toDate("2024-05-08T14:30:45Z"));

        assertThat(RequestUtil.parseLongDate("2024-05-08T14:30:45.123Z"))
                .isEqualTo(toDate("2024-05-08T14:30:45.123Z"));

        // positive offset
        assertThat(RequestUtil.parseLongDate("2024-05-08T14:30+02:00"))
                .isEqualTo(toDate("2024-05-08T12:30:00Z"));

        assertThat(RequestUtil.parseLongDate("2024-05-08T14:30:45+02:00"))
                .isEqualTo(toDate("2024-05-08T12:30:45Z"));

        assertThat(RequestUtil.parseLongDate("2024-05-08T14:30:45.123+02:00"))
                .isEqualTo(toDate("2024-05-08T12:30:45.123Z"));
    }

    @Test
    public void testInvalidDates() {
        // Missing minutes or malformed time still invalid
        Assertions.assertThrows(Exception.class, () -> RequestUtil.parseLongDate("2024-05-08T14"));
        Assertions.assertThrows(Exception.class, () -> RequestUtil.parseLongDate("2024-05-08T:30"));
        Assertions.assertThrows(Exception.class, () -> RequestUtil.parseLongDate("May 8, 2024"));
        Assertions.assertThrows(Exception.class, () -> RequestUtil.parseLongDate("2024-05-08T14-30"));
        // null input still NPE
        Assertions.assertThrows(NullPointerException.class, () -> RequestUtil.parseLongDate(null));
    }

    protected Date toDate(String dateString) {
        return Date.from(Instant.parse(dateString));
    }
}
