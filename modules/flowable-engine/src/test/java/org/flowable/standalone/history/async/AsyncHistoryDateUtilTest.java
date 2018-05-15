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

import java.util.Calendar;
import java.util.Date;

import org.flowable.job.service.impl.history.async.AsyncHistoryDateUtil;
import org.junit.Assert;
import org.junit.Test;

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
    Assert.assertNotNull(parsedDate);

    calendar.setTime(parsedDate);
    Assert.assertEquals(year, calendar.get(Calendar.YEAR));
    Assert.assertEquals(month, calendar.get(Calendar.MONTH));
    Assert.assertEquals(day, calendar.get(Calendar.DAY_OF_MONTH));
    Assert.assertEquals(hour, calendar.get(Calendar.HOUR));
    Assert.assertEquals(min, calendar.get(Calendar.MINUTE));
    Assert.assertEquals(second, calendar.get(Calendar.SECOND));
    Assert.assertEquals(milliSecond, calendar.get(Calendar.MILLISECOND));
  }

}
