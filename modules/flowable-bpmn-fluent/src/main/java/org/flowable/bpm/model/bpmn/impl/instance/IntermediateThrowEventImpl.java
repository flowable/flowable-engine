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

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_INTERMEDIATE_THROW_EVENT;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.builder.IntermediateThrowEventBuilder;
import org.flowable.bpm.model.bpmn.impl.BpmnModelConstants;
import org.flowable.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.flowable.bpm.model.bpmn.instance.ThrowEvent;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;

/**
 * The BPMN intermediateThrowEvent element.
 */
public class IntermediateThrowEventImpl
        extends ThrowEventImpl
        implements IntermediateThrowEvent {

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(IntermediateThrowEvent.class, BPMN_ELEMENT_INTERMEDIATE_THROW_EVENT)
                .namespaceUri(BpmnModelConstants.BPMN20_NS)
                .extendsType(ThrowEvent.class)
                .instanceProvider(new ModelTypeInstanceProvider<IntermediateThrowEvent>() {
                    public IntermediateThrowEvent newInstance(ModelTypeInstanceContext instanceContext) {
                        return new IntermediateThrowEventImpl(instanceContext);
                    }
                });

        typeBuilder.build();
    }

    public IntermediateThrowEventImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public IntermediateThrowEventBuilder builder() {
        return new IntermediateThrowEventBuilder((BpmnModelInstance) modelInstance, this);
    }
}
