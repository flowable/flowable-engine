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

package org.flowable.engine.impl.form;

import java.text.ParseException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.form.AbstractFormType;

/**
 * @author Tom Baeyens
 */
public class DateFormType extends AbstractFormType {

    private static final long serialVersionUID = 1L;

    protected String datePattern;
    protected boolean lenientDateParsing;
    protected FastDateFormat dateFormat;

    public DateFormType(String datePattern) {
        this(datePattern, false);
    }

    public DateFormType(String datePattern, boolean lenientDateParsing) {
        this.datePattern = datePattern;
        this.lenientDateParsing = lenientDateParsing;
        this.dateFormat = FastDateFormat.getInstance(datePattern);
    }

    @Override
    public String getName() {
        return "date";
    }

    @Override
    public Object getInformation(String key) {
        if ("datePattern".equals(key)) {
            return datePattern;
        }
        return null;
    }

    @Override
    public Object convertFormValueToModelValue(String propertyValue) {
        if (StringUtils.isEmpty(propertyValue)) {
            return null;
        }
        try {
            if (lenientDateParsing) {
                return dateFormat.parseObject(propertyValue);
            }
            // FastDateFormat is always lenient, so strict parsing uses DateUtils,
            // which also rejects input with trailing characters after the date.
            return DateUtils.parseDateStrictly(propertyValue, datePattern);
        } catch (ParseException e) {
            throw new FlowableIllegalArgumentException("invalid date value " + propertyValue, e);
        }
    }

    @Override
    public String convertModelValueToFormValue(Object modelValue) {
        if (modelValue == null) {
            return null;
        }
        return dateFormat.format(modelValue);
    }
}
