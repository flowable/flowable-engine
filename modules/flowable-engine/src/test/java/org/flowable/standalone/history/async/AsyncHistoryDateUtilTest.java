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
package org.flowable.standalone.history.async;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Calendar;
import java.util.Date;

import org.flowable.job.service.impl.history.async.AsyncHistoryDateUtil;
import org.junit.jupiter.api.Test;

public class AsyncHistoryDateUtilTest {

    @Test
    public void testFormatAndParseISO8601Date() {
        Calendar calendar = Calendar.getInstance();

        Date date = new Date();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR);
        int min = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int milliSecond = calendar.get(Calendar.MILLISECOND);

        String s = AsyncHistoryDateUtil.formatDate(date);
        Date parsedDate = AsyncHistoryDateUtil.parseDate(s);
        assertThat(parsedDate).isNotNull();

        calendar.setTime(parsedDate);
        assertThat(calendar.get(Calendar.YEAR)).isEqualTo(year);
        assertThat(calendar.get(Calendar.MONTH)).isEqualTo(month);
        assertThat(calendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(day);
        assertThat(calendar.get(Calendar.HOUR)).isEqualTo(hour);
        assertThat(calendar.get(Calendar.MINUTE)).isEqualTo(min);
        assertThat(calendar.get(Calendar.SECOND)).isEqualTo(second);
        assertThat(calendar.get(Calendar.MILLISECOND)).isEqualTo(milliSecond);
    }

}
