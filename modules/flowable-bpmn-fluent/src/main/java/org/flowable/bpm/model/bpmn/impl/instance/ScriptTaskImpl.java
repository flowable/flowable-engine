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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_SCRIPT_FORMAT;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_SCRIPT_TASK;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_RESULT_VARIABLE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.builder.ScriptTaskBuilder;
import org.flowable.bpm.model.bpmn.instance.Script;
import org.flowable.bpm.model.bpmn.instance.ScriptTask;
import org.flowable.bpm.model.bpmn.instance.Task;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

/**
 * The BPMN scriptTask element.
 */
public class ScriptTaskImpl
        extends TaskImpl
        implements ScriptTask {

    protected static Attribute<String> scriptFormatAttribute;
    protected static ChildElement<Script> scriptChild;

    /* Flowable extensions */

    protected static Attribute<String> flowableResultVariableAttribute;
    protected static Attribute<String> flowableResourceAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ScriptTask.class, BPMN_ELEMENT_SCRIPT_TASK)
                .namespaceUri(BPMN20_NS)
                .extendsType(Task.class)
                .instanceProvider(new ModelTypeInstanceProvider<ScriptTask>() {
                    public ScriptTask newInstance(ModelTypeInstanceContext instanceContext) {
                        return new ScriptTaskImpl(instanceContext);
                    }
                });

        scriptFormatAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_SCRIPT_FORMAT)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        scriptChild = sequenceBuilder.element(Script.class)
                .build();

        /* Flowable extensions */

        flowableResultVariableAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_RESULT_VARIABLE)
                .namespace(FLOWABLE_NS)
                .build();

        typeBuilder.build();
    }

    public ScriptTaskImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public ScriptTaskBuilder builder() {
        return new ScriptTaskBuilder((BpmnModelInstance) modelInstance, this);
    }

    public Script getScript() {
        return scriptChild.getChild(this);
    }

    public void setScript(Script script) {
        scriptChild.setChild(this, script);
    }

    /* Flowable extensions */

    public String getFlowableResultVariable() {
        return flowableResultVariableAttribute.getValue(this);
    }

    public void setFlowableResultVariable(String flowableResultVariable) {
        flowableResultVariableAttribute.setValue(this, flowableResultVariable);
    }
}
