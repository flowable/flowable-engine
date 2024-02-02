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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.FormProperty;
import org.flowable.bpmn.model.FormValue;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.form.AbstractFormType;

/**
 * @author Tom Baeyens
 */
public class FormTypes {

    protected Map<String, AbstractFormType> formTypes = new HashMap<>();

    public void addFormType(AbstractFormType formType) {
        formTypes.put(formType.getName(), formType);
    }

    public AbstractFormType parseFormPropertyType(FormProperty formProperty) {
        AbstractFormType formType = null;

        if ("date".equals(formProperty.getType()) && StringUtils.isNotEmpty(formProperty.getDatePattern())) {
            formType = new DateFormType(formProperty.getDatePattern());

        } else if ("enum".equals(formProperty.getType())) {
            // ACT-1023: Using linked hashmap to preserve the order in which the
            // entries are defined
            Map<String, String> values = new LinkedHashMap<>();
            for (FormValue formValue : formProperty.getFormValues()) {
                values.put(formValue.getId(), formValue.getName());
            }
            formType = new EnumFormType(values);

        } else if (StringUtils.isNotEmpty(formProperty.getType())) {
            formType = formTypes.get(formProperty.getType());
            if (formType == null) {
                throw new FlowableIllegalArgumentException("unknown type '" + formProperty.getType() + "' " + formProperty.getId());
            }
        }
        return formType;
    }
}
