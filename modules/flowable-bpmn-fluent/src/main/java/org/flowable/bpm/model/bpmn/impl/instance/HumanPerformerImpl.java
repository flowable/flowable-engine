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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_HUMAN_PERFORMER;

import org.flowable.bpm.model.bpmn.instance.HumanPerformer;
import org.flowable.bpm.model.bpmn.instance.Performer;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;

/**
 * The BPMN humanPerformer element.
 */
public class HumanPerformerImpl
        extends PerformerImpl
        implements HumanPerformer {

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(HumanPerformer.class, BPMN_ELEMENT_HUMAN_PERFORMER)
                .namespaceUri(BPMN20_NS)
                .extendsType(Performer.class)
                .instanceProvider(new ModelElementTypeBuilder.ModelTypeInstanceProvider<HumanPerformer>() {
                    @Override
                    public HumanPerformer newInstance(ModelTypeInstanceContext instanceContext) {
                        return new HumanPerformerImpl(instanceContext);
                    }
                });
        typeBuilder.build();
    }

    public HumanPerformerImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

}
