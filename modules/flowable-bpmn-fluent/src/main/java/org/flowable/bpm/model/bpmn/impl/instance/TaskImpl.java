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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_TASK;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_ASYNC;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.builder.AbstractTaskBuilder;
import org.flowable.bpm.model.bpmn.instance.Activity;
import org.flowable.bpm.model.bpmn.instance.Task;
import org.flowable.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.impl.util.ModelTypeException;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;

/**
 * The BPMN task element.
 */
public class TaskImpl
        extends ActivityImpl
        implements Task {

    /* Flowable extensions */
    protected static Attribute<Boolean> flowableAsyncAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Task.class, BPMN_ELEMENT_TASK)
                .namespaceUri(BPMN20_NS)
                .extendsType(Activity.class)
                .instanceProvider(new ModelTypeInstanceProvider<Task>() {
                    public Task newInstance(ModelTypeInstanceContext instanceContext) {
                        return new TaskImpl(instanceContext);
                    }
                });

        flowableAsyncAttribute = typeBuilder.booleanAttribute(FLOWABLE_ATTRIBUTE_ASYNC)
                .namespace(FLOWABLE_NS)
                .defaultValue(false)
                .build();

        typeBuilder.build();
    }

    public TaskImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @SuppressWarnings("rawtypes")
    public AbstractTaskBuilder builder() {
        throw new ModelTypeException("No builder implemented.");
    }

    public BpmnShape getDiagramElement() {
        return (BpmnShape) super.getDiagramElement();
    }

}
