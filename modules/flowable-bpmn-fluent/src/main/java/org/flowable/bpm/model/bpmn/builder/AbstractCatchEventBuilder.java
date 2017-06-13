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
package org.flowable.bpm.model.bpmn.builder;

import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.instance.CatchEvent;
import org.flowable.bpm.model.bpmn.instance.CompensateEventDefinition;
import org.flowable.bpm.model.bpmn.instance.ConditionalEventDefinition;
import org.flowable.bpm.model.bpmn.instance.MessageEventDefinition;
import org.flowable.bpm.model.bpmn.instance.SignalEventDefinition;
import org.flowable.bpm.model.bpmn.instance.TimeCycle;
import org.flowable.bpm.model.bpmn.instance.TimeDate;
import org.flowable.bpm.model.bpmn.instance.TimeDuration;
import org.flowable.bpm.model.bpmn.instance.TimerEventDefinition;

public abstract class AbstractCatchEventBuilder<B extends AbstractCatchEventBuilder<B, E>, E extends CatchEvent>
        extends AbstractEventBuilder<B, E> {

    protected AbstractCatchEventBuilder(BpmnModelInstance modelInstance, E element, Class<?> selfType) {
        super(modelInstance, element, selfType);
    }

    /**
     * Sets the event to be parallel multiple
     *
     * @return the builder object
     */
    public B parallelMultiple() {
        element.isParallelMultiple();
        return myself;
    }

    /**
     * Sets an event definition for the given message name. If already a message with this name exists it will be used, otherwise a new message is
     * created.
     *
     * @param messageName the name of the message
     * @return the builder object
     */
    public B message(String messageName) {
        MessageEventDefinition messageEventDefinition = createMessageEventDefinition(messageName);
        element.getEventDefinitions().add(messageEventDefinition);

        return myself;
    }

    /**
     * Sets an event definition for the given signal name. If already a signal with this name exists it will be used, otherwise a new signal is
     * created.
     *
     * @param signalName the name of the signal
     * @return the builder object
     */
    public B signal(String signalName) {
        SignalEventDefinition signalEventDefinition = createSignalEventDefinition(signalName);
        element.getEventDefinitions().add(signalEventDefinition);

        return myself;
    }


    /**
     * Sets an event definition for the timer with a time date.
     *
     * @param timerDate the time date of the timer
     * @return the builder object
     */
    public B timerWithDate(String timerDate) {
        TimeDate timeDate = createInstance(TimeDate.class);
        timeDate.setTextContent(timerDate);

        TimerEventDefinition timerEventDefinition = createInstance(TimerEventDefinition.class);
        timerEventDefinition.setTimeDate(timeDate);

        element.getEventDefinitions().add(timerEventDefinition);

        return myself;
    }

    /**
     * Sets an event definition for the timer with a time duration.
     *
     * @param timerDuration the time duration of the timer
     * @return the builder object
     */
    public B timerWithDuration(String timerDuration) {
        TimeDuration timeDuration = createInstance(TimeDuration.class);
        timeDuration.setTextContent(timerDuration);

        TimerEventDefinition timerEventDefinition = createInstance(TimerEventDefinition.class);
        timerEventDefinition.setTimeDuration(timeDuration);

        element.getEventDefinitions().add(timerEventDefinition);

        return myself;
    }

    /**
     * Sets an event definition for the timer with a time cycle.
     *
     * @param timerCycle the time cycle of the timer
     * @return the builder object
     */
    public B timerWithCycle(String timerCycle) {
        TimeCycle timeCycle = createInstance(TimeCycle.class);
        timeCycle.setTextContent(timerCycle);

        TimerEventDefinition timerEventDefinition = createInstance(TimerEventDefinition.class);
        timerEventDefinition.setTimeCycle(timeCycle);

        element.getEventDefinitions().add(timerEventDefinition);

        return myself;
    }

    public CompensateEventDefinitionBuilder compensateEventDefinition() {
        return compensateEventDefinition(null);
    }

    public CompensateEventDefinitionBuilder compensateEventDefinition(String id) {
        CompensateEventDefinition eventDefinition = createInstance(CompensateEventDefinition.class);
        if (id != null) {
            eventDefinition.setId(id);
        }

        element.getEventDefinitions().add(eventDefinition);
        return new CompensateEventDefinitionBuilder(modelInstance, eventDefinition);
    }

    public ConditionalEventDefinitionBuilder conditionalEventDefinition() {
        return conditionalEventDefinition(null);
    }

    public ConditionalEventDefinitionBuilder conditionalEventDefinition(String id) {
        ConditionalEventDefinition eventDefinition = createInstance(ConditionalEventDefinition.class);
        if (id != null) {
            eventDefinition.setId(id);
        }

        element.getEventDefinitions().add(eventDefinition);
        return new ConditionalEventDefinitionBuilder(modelInstance, eventDefinition);
    }

    public B condition(String condition) {
        conditionalEventDefinition().condition(condition);
        return myself;
    }

}
