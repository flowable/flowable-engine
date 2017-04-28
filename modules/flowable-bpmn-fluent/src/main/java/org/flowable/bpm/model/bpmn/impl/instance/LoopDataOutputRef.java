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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_LOOP_DATA_OUTPUT_REF;

import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;

/**
 * The BPMN 2.0 loopDataOutputRef element of the BPMN 2.0 tMultiInstanceLoopCharacteristics type.
 */
public class LoopDataOutputRef
        extends BpmnModelElementInstanceImpl {

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder
                .defineType(LoopDataOutputRef.class, BPMN_ELEMENT_LOOP_DATA_OUTPUT_REF)
                .namespaceUri(BPMN20_NS)
                .instanceProvider(
                        new ModelElementTypeBuilder.ModelTypeInstanceProvider<LoopDataOutputRef>() {
                            @Override
                            public LoopDataOutputRef newInstance(ModelTypeInstanceContext instanceContext) {
                                return new LoopDataOutputRef(instanceContext);
                            }
                        });

        typeBuilder.build();
    }

    public LoopDataOutputRef(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }
}
