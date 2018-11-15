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
package org.flowable.cmmn.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class FlowableListener extends CmmnElement {

    protected String event;
    protected String sourceState;
    protected String targetState;
    protected String implementationType;
    protected String implementation;
    protected List<FieldExtension> fieldExtensions = new ArrayList<>();
    protected String onTransaction;

    @JsonIgnore
    protected Object instance; // Can be used to set an instance of the listener directly. That instance will then always be reused.
    
    public FlowableListener() {
        // Always generate a random identifier to look up the listener while executing the logic
        setId(UUID.randomUUID().toString());
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getSourceState() {
        return sourceState;
    }

    public void setSourceState(String sourceState) {
        this.sourceState = sourceState;
    }

    public String getTargetState() {
        return targetState;
    }

    public void setTargetState(String targetState) {
        this.targetState = targetState;
    }

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

    public String getOnTransaction() {
        return onTransaction;
    }

    public void setOnTransaction(String onTransaction) {
        this.onTransaction = onTransaction;
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    @Override
    public FlowableListener clone() {
        FlowableListener clone = new FlowableListener();
        clone.setValues(this);
        return clone;
    }

    public void setValues(FlowableListener otherListener) {
        setEvent(otherListener.getEvent());
        setImplementation(otherListener.getImplementation());
        setImplementationType(otherListener.getImplementationType());

        fieldExtensions = new ArrayList<>();
        if (otherListener.getFieldExtensions() != null && !otherListener.getFieldExtensions().isEmpty()) {
            for (FieldExtension extension : otherListener.getFieldExtensions()) {
                fieldExtensions.add(extension.clone());
            }
        }
    }
}
