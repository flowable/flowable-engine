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
package org.flowable.bpm.model.bpmn.impl.instance;

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_TIMER_EVENT_DEFINITION;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.EventDefinition;
import org.flowable.bpm.model.bpmn.instance.TimeCycle;
import org.flowable.bpm.model.bpmn.instance.TimeDate;
import org.flowable.bpm.model.bpmn.instance.TimeDuration;
import org.flowable.bpm.model.bpmn.instance.TimerEventDefinition;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

/**
 * The BPMN timerEventDefinition element.
 */
public class TimerEventDefinitionImpl
        extends EventDefinitionImpl
        implements TimerEventDefinition {

    protected static ChildElement<TimeDate> timeDateChild;
    protected static ChildElement<TimeDuration> timeDurationChild;
    protected static ChildElement<TimeCycle> timeCycleChild;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(TimerEventDefinition.class, BPMN_ELEMENT_TIMER_EVENT_DEFINITION)
                .namespaceUri(BPMN20_NS)
                .extendsType(EventDefinition.class)
                .instanceProvider(new ModelTypeInstanceProvider<TimerEventDefinition>() {
                    public TimerEventDefinition newInstance(ModelTypeInstanceContext instanceContext) {
                        return new TimerEventDefinitionImpl(instanceContext);
                    }
                });

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        timeDateChild = sequenceBuilder.element(TimeDate.class)
                .build();

        timeDurationChild = sequenceBuilder.element(TimeDuration.class)
                .build();

        timeCycleChild = sequenceBuilder.element(TimeCycle.class)
                .build();

        typeBuilder.build();
    }

    public TimerEventDefinitionImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    public TimeDate getTimeDate() {
        return timeDateChild.getChild(this);
    }

    public void setTimeDate(TimeDate timeDate) {
        timeDateChild.setChild(this, timeDate);
    }

    public TimeDuration getTimeDuration() {
        return timeDurationChild.getChild(this);
    }

    public void setTimeDuration(TimeDuration timeDuration) {
        timeDurationChild.setChild(this, timeDuration);
    }

    public TimeCycle getTimeCycle() {
        return timeCycleChild.getChild(this);
    }

    public void setTimeCycle(TimeCycle timeCycle) {
        timeCycleChild.setChild(this, timeCycle);
    }

}
