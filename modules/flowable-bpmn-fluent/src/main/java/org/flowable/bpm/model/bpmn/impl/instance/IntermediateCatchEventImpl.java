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

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_INTERMEDIATE_CATCH_EVENT;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.builder.IntermediateCatchEventBuilder;
import org.flowable.bpm.model.bpmn.impl.BpmnModelConstants;
import org.flowable.bpm.model.bpmn.instance.CatchEvent;
import org.flowable.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;

/**
 * The BPMN intermediateCatchEvent element.
 */
public class IntermediateCatchEventImpl
        extends CatchEventImpl
        implements IntermediateCatchEvent {

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(IntermediateCatchEvent.class, BPMN_ELEMENT_INTERMEDIATE_CATCH_EVENT)
                .namespaceUri(BpmnModelConstants.BPMN20_NS)
                .extendsType(CatchEvent.class)
                .instanceProvider(new ModelTypeInstanceProvider<IntermediateCatchEvent>() {
                    public IntermediateCatchEvent newInstance(ModelTypeInstanceContext instanceContext) {
                        return new IntermediateCatchEventImpl(instanceContext);
                    }
                });

        typeBuilder.build();
    }

    public IntermediateCatchEventImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public IntermediateCatchEventBuilder builder() {
        return new IntermediateCatchEventBuilder((BpmnModelInstance) modelInstance, this);
    }
}
