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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * @author Tijs Rademakers
 */
public class MessageEventDefinition extends EventDefinition {

    private static final Set<EventDefinitionLocation> SUPPORTED_LOCATIONS = Collections.unmodifiableSet(EnumSet.of(
            EventDefinitionLocation.START_EVENT,
            EventDefinitionLocation.EVENT_SUBPROCESS_START_EVENT,
            EventDefinitionLocation.INTERMEDIATE_CATCH_EVENT,
            EventDefinitionLocation.BOUNDARY_EVENT));

    protected String messageRef;
    protected String messageExpression;

    @Override
    public Set<EventDefinitionLocation> getSupportedLocations() {
        return SUPPORTED_LOCATIONS;
    }

    public String getMessageRef() {
        return messageRef;
    }

    public void setMessageRef(String messageRef) {
        this.messageRef = messageRef;
    }

    public String getMessageExpression() {
        return messageExpression;
    }

    public void setMessageExpression(String messageExpression) {
        this.messageExpression = messageExpression;
    }

    @Override
    public MessageEventDefinition clone() {
        MessageEventDefinition clone = new MessageEventDefinition();
        clone.setValues(this);
        return clone;
    }

    public void setValues(MessageEventDefinition otherDefinition) {
        super.setValues(otherDefinition);
        setMessageRef(otherDefinition.getMessageRef());
        setMessageExpression(otherDefinition.getMessageExpression());
    }
}
