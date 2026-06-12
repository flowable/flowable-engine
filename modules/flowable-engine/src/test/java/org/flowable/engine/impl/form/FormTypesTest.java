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

import org.flowable.bpmn.model.FormProperty;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.form.AbstractFormType;
import org.junit.jupiter.api.Test;

class FormTypesTest {

    private static final String INVALID_DATE = "15/13/2024";

    private final FormTypes formTypes = new FormTypes();

    @Test
    void dateParsingIsStrictByDefault() {
        AbstractFormType formType = formTypes.parseFormPropertyType(dateFormProperty(null));
        assertThatThrownBy(() -> formType.convertFormValueToModelValue(INVALID_DATE))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    void engineDefaultLenientIsApplied() {
        formTypes.setLenientDateParsing(true);
        AbstractFormType formType = formTypes.parseFormPropertyType(dateFormProperty(null));
        assertThat(formType.convertFormValueToModelValue(INVALID_DATE)).isInstanceOf(Date.class);
    }

    @Test
    void formPropertyOverrideWinsOverStrictDefault() {
        AbstractFormType formType = formTypes.parseFormPropertyType(dateFormProperty(true));
        assertThat(formType.convertFormValueToModelValue(INVALID_DATE)).isInstanceOf(Date.class);
    }

    @Test
    void formPropertyOverrideWinsOverLenientDefault() {
        formTypes.setLenientDateParsing(true);
        AbstractFormType formType = formTypes.parseFormPropertyType(dateFormProperty(false));
        assertThatThrownBy(() -> formType.convertFormValueToModelValue(INVALID_DATE))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    private FormProperty dateFormProperty(Boolean lenientDateParsing) {
        FormProperty formProperty = new FormProperty();
        formProperty.setId("dateProperty");
        formProperty.setType("date");
        formProperty.setDatePattern("dd/MM/yyyy");
        formProperty.setLenientDateParsing(lenientDateParsing);
        return formProperty;
    }
}
