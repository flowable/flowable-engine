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
package org.flowable.bpmn.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tijs Rademakers
 */
public class StartEvent extends Event {

    protected String initiator;
    protected String formKey;
    protected boolean isInterrupting = true;
    protected String validateFormFields;
    protected List<FormProperty> formProperties = new ArrayList<>();

    public String getInitiator() {
        return initiator;
    }

    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }

    public String getFormKey() {
        return formKey;
    }

    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }

    public boolean isInterrupting() {
        return isInterrupting;
    }

    public void setInterrupting(boolean isInterrupting) {
        this.isInterrupting = isInterrupting;
    }

    public List<FormProperty> getFormProperties() {
        return formProperties;
    }

    public void setFormProperties(List<FormProperty> formProperties) {
        this.formProperties = formProperties;
    }

    public String getValidateFormFields() {
        return validateFormFields;
    }

    public void setValidateFormFields(String validateFormFields) {
        this.validateFormFields = validateFormFields;
    }

    @Override
    public StartEvent clone() {
        StartEvent clone = new StartEvent();
        clone.setValues(this);
        return clone;
    }

    public void setValues(StartEvent otherEvent) {
        super.setValues(otherEvent);
        setInitiator(otherEvent.getInitiator());
        setFormKey(otherEvent.getFormKey());
        setInterrupting(otherEvent.isInterrupting);
        setValidateFormFields(otherEvent.validateFormFields);

        formProperties = new ArrayList<>();
        if (otherEvent.getFormProperties() != null && !otherEvent.getFormProperties().isEmpty()) {
            for (FormProperty property : otherEvent.getFormProperties()) {
                formProperties.add(property.clone());
            }
        }
    }
}
