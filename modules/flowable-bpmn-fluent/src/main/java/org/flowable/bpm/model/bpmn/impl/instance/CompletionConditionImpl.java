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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_COMPLETION_CONDITION;

import org.flowable.bpm.model.bpmn.instance.CompletionCondition;
import org.flowable.bpm.model.bpmn.instance.Expression;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;

/**
 * The BPMN 2.0 completionCondition element from the tMultiInstanceLoopCharacteristics type.
 */
public class CompletionConditionImpl
        extends ExpressionImpl
        implements CompletionCondition {

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder
                .defineType(CompletionCondition.class, BPMN_ELEMENT_COMPLETION_CONDITION)
                .namespaceUri(BPMN20_NS)
                .extendsType(Expression.class)
                .instanceProvider(
                        new ModelElementTypeBuilder.ModelTypeInstanceProvider<CompletionCondition>() {
                            @Override
                            public CompletionCondition newInstance(ModelTypeInstanceContext instanceContext) {
                                return new CompletionConditionImpl(instanceContext);
                            }
                        });

        typeBuilder.build();
    }

    public CompletionConditionImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

}
