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

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author Tijs Rademakers
 */
public abstract class AbstractFlowableHttpHandler extends BaseElement {

    protected String implementationType;
    protected String implementation;
    protected List<FieldExtension> fieldExtensions = new ArrayList<>();

    @JsonIgnore
    protected Object instance; // Can be used to set an instance of the listener directly. That instance will then always be reused.

    public String getImplementationType() {
        return implementationType;
    }

    public void setImplementationType(String implementationType) {
        this.implementationType = implementationType;
    }

    public String getImplementation() {
        return implementation;
    }

    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

    public List<FieldExtension> getFieldExtensions() {
        return fieldExtensions;
    }

    public void setFieldExtensions(List<FieldExtension> fieldExtensions) {
        this.fieldExtensions = fieldExtensions;
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    @Override
    public abstract AbstractFlowableHttpHandler clone();

    public void setValues(AbstractFlowableHttpHandler otherHandler) {
        setImplementation(otherHandler.getImplementation());
        setImplementationType(otherHandler.getImplementationType());

        fieldExtensions = new ArrayList<>();
        if (otherHandler.getFieldExtensions() != null && !otherHandler.getFieldExtensions().isEmpty()) {
            for (FieldExtension extension : otherHandler.getFieldExtensions()) {
                fieldExtensions.add(extension.clone());
            }
        }
    }
}
