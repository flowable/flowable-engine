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

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;

public class AsyncHistoryDateUtil {

    public static String formatDate(Date date) {
        if (date != null) {
            return date.toInstant().toString();
        }
        return null;
    }

    public static Date parseDate(String s) {
        if (s != null) {
            try {
                return Date.from(Instant.parse(s));
            } catch (DateTimeParseException e) {
                return null;
            }
        }
        return null;
    }

}
