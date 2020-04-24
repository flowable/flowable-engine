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
public abstract class Event extends FlowNode {

    protected List<EventDefinition> eventDefinitions = new ArrayList<>();
    protected List<IOParameter> inParameters = new ArrayList<>();
    protected List<IOParameter> outParameters = new ArrayList<>();

    public List<EventDefinition> getEventDefinitions() {
        return eventDefinitions;
    }

    public void setEventDefinitions(List<EventDefinition> eventDefinitions) {
        this.eventDefinitions = eventDefinitions;
    }

    public void addEventDefinition(EventDefinition eventDefinition) {
        eventDefinitions.add(eventDefinition);
    }
    
    public List<IOParameter> getInParameters() {
        return inParameters;
    }

    public void setInParameters(List<IOParameter> inParameters) {
        this.inParameters = inParameters;
    }

    public List<IOParameter> getOutParameters() {
        return outParameters;
    }

    public void setOutParameters(List<IOParameter> outParameters) {
        this.outParameters = outParameters;
    }

    public void setValues(Event otherEvent) {
        super.setValues(otherEvent);

        eventDefinitions = new ArrayList<>();
        if (otherEvent.getEventDefinitions() != null && !otherEvent.getEventDefinitions().isEmpty()) {
            for (EventDefinition eventDef : otherEvent.getEventDefinitions()) {
                eventDefinitions.add(eventDef.clone());
            }
        }
        
        inParameters = new ArrayList<>();
        if (otherEvent.getInParameters() != null && !otherEvent.getInParameters().isEmpty()) {
            for (IOParameter parameter : otherEvent.getInParameters()) {
                inParameters.add(parameter.clone());
            }
        }

        outParameters = new ArrayList<>();
        if (otherEvent.getOutParameters() != null && !otherEvent.getOutParameters().isEmpty()) {
            for (IOParameter parameter : otherEvent.getOutParameters()) {
                outParameters.add(parameter.clone());
            }
        }
    }
}
