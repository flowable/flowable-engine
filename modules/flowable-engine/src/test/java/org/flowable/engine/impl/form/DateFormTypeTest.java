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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.junit.jupiter.api.Test;

class DateFormTypeTest {

    private final DateFormType dateFormType = new DateFormType("dd/MM/yyyy");

    @Test
    void nullValueReturnsNull() {
        assertThat(dateFormType.convertFormValueToModelValue(null)).isNull();
    }

    @Test
    void emptyValueReturnsNull() {
        assertThat(dateFormType.convertFormValueToModelValue("")).isNull();
    }

    @Test
    void validDateIsParsed() {
        Object result = dateFormType.convertFormValueToModelValue("15/06/2024");
        assertThat(result).isInstanceOf(Date.class);
    }

    @Test
    void invalidMonthThrowsException() {
        assertThatThrownBy(() -> dateFormType.convertFormValueToModelValue("15/13/2024"))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    void invalidDayThrowsException() {
        assertThatThrownBy(() -> dateFormType.convertFormValueToModelValue("32/06/2024"))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    void wrongFormatThrowsException() {
        assertThatThrownBy(() -> dateFormType.convertFormValueToModelValue("2024-06-15"))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    void differentFormatValidDateIsParsed() {
        DateFormType isoFormat = new DateFormType("yyyy-MM-dd");
        Object result = isoFormat.convertFormValueToModelValue("2024-06-15");
        assertThat(result).isInstanceOf(Date.class);
    }

    @Test
    void differentFormatInvalidDayThrowsException() {
        DateFormType isoFormat = new DateFormType("yyyy-MM-dd");
        assertThatThrownBy(() -> isoFormat.convertFormValueToModelValue("2024-06-32"))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    void inputValidForOtherFormatThrowsException() {
        DateFormType isoFormat = new DateFormType("yyyy-MM-dd");
        assertThatThrownBy(() -> isoFormat.convertFormValueToModelValue("15/06/2024"))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

}
