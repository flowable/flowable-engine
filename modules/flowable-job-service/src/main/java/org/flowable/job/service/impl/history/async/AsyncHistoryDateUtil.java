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
package org.flowable.job.service.impl.history.async;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.util.ISO8601Utils;

public class AsyncHistoryDateUtil {

    protected static TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");

    public static String formatDate(Date date) {
        if (date != null) {
            return ISO8601Utils.format(date, true, utcTimeZone);
        }
        return null;
    }

    public static Date parseDate(String s) {
        if (s != null) {
            try {
                return ISO8601Utils.parse(s, new ParsePosition(0));
            } catch (ParseException e) {
                return null;
            }
        }
        return null;
    }

}
