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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_PARALLEL_GATEWAY;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_ASYNC;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.builder.ParallelGatewayBuilder;
import org.flowable.bpm.model.bpmn.instance.Gateway;
import org.flowable.bpm.model.bpmn.instance.ParallelGateway;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;

/**
 * The BPMN parallelGateway element.
 */
public class ParallelGatewayImpl
        extends GatewayImpl
        implements ParallelGateway {

    protected static Attribute<Boolean> flowableAsyncAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ParallelGateway.class, BPMN_ELEMENT_PARALLEL_GATEWAY)
                .namespaceUri(BPMN20_NS)
                .extendsType(Gateway.class)
                .instanceProvider(new ModelTypeInstanceProvider<ParallelGateway>() {
                    @Override
                    public ParallelGateway newInstance(ModelTypeInstanceContext instanceContext) {
                        return new ParallelGatewayImpl(instanceContext);
                    }
                });


        flowableAsyncAttribute = typeBuilder.booleanAttribute(FLOWABLE_ATTRIBUTE_ASYNC)
                .namespace(FLOWABLE_NS)
                .defaultValue(false)
                .build();

        typeBuilder.build();
    }

    @Override
    public ParallelGatewayBuilder builder() {
        return new ParallelGatewayBuilder((BpmnModelInstance) modelInstance, this);
    }

    public ParallelGatewayImpl(ModelTypeInstanceContext context) {
        super(context);
    }

}
