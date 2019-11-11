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
public class SendEventServiceTask extends ServiceTask {

    protected String eventType;
    protected String triggerEventType;
    protected boolean sendSynchronously;
    protected List<IOParameter> eventInParameters = new ArrayList<>();
    protected List<IOParameter> eventOutParameters = new ArrayList<>();

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTriggerEventType() {
        return triggerEventType;
    }

    public void setTriggerEventType(String triggerEventType) {
        this.triggerEventType = triggerEventType;
    }

    public boolean isSendSynchronously() {
        return sendSynchronously;
    }

    public void setSendSynchronously(boolean sendSynchronously) {
        this.sendSynchronously = sendSynchronously;
    }

    public List<IOParameter> getEventInParameters() {
        return eventInParameters;
    }

    public void setEventInParameters(List<IOParameter> eventInParameters) {
        this.eventInParameters = eventInParameters;
    }

    public List<IOParameter> getEventOutParameters() {
        return eventOutParameters;
    }

    public void setEventOutParameters(List<IOParameter> eventOutParameters) {
        this.eventOutParameters = eventOutParameters;
    }

    @Override
    public SendEventServiceTask clone() {
        SendEventServiceTask clone = new SendEventServiceTask();
        clone.setValues(this);
        return clone;
    }

    public void setValues(SendEventServiceTask otherElement) {
        super.setValues(otherElement);
        setEventType(otherElement.getEventType());
        setTriggerEventType(otherElement.getTriggerEventType());
        setSendSynchronously(otherElement.isSendSynchronously());
        
        eventInParameters = new ArrayList<>();
        if (otherElement.getEventInParameters() != null && !otherElement.getEventInParameters().isEmpty()) {
            for (IOParameter parameter : otherElement.getEventInParameters()) {
                eventInParameters.add(parameter.clone());
            }
        }

        eventOutParameters = new ArrayList<>();
        if (otherElement.getEventOutParameters() != null && !otherElement.getEventOutParameters().isEmpty()) {
            for (IOParameter parameter : otherElement.getEventOutParameters()) {
                eventOutParameters.add(parameter.clone());
            }
        }
    }
}
