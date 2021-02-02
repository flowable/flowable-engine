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
package org.flowable.editor.language.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CollectionHandler;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.model.VariableAggregationDefinition;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

/**
 * @see <a href="https://github.com/flowable/flowable-engine/issues/474">Issue 474</a>
 */
class MultiInstanceTaskConverterTest {
	private static final String PARTICIPANT_VALUE = "[\n" +
"                   {\n" +
"                     \"principalType\" : \"User\",\n" +
"                     \"role\" : \"PotentialOwner\",\n" +
"                     \"principal\" : \"wfuser1\",\n" +
"                     \"version\" : 1\n" +
"                   },\n" +
"                   {\n" +
"                     \"principalType\" : \"User\",\n" +
"                     \"role\" : \"PotentialOwner\",\n" +
"                     \"principal\" : \"wfuser2\",\n" +
"                     \"version\" : 1\n" +
"                   }\n" +
"                 ]";

    @BpmnXmlConverterTest("multiinstancemodel.bpmn")
    void validateModel(BpmnModel model) {
        Process main = model.getMainProcess();

        // verify start
        FlowElement flowElement = main.getFlowElement("start1");
        assertThat(flowElement)
                .isInstanceOfSatisfying(StartEvent.class, startEvent -> {
                    assertThat(startEvent.getId()).isEqualTo("start1");
                });

        // verify user task
        flowElement = main.getFlowElement("userTask1");
        assertThat(flowElement)
                .isInstanceOfSatisfying(UserTask.class, userTask -> {
                    assertThat(userTask.getName()).isEqualTo("User task 1");
                    MultiInstanceLoopCharacteristics loopCharacteristics = userTask.getLoopCharacteristics();
                    assertThat(loopCharacteristics.getElementVariable()).isEqualTo("participant");
                    assertThat(loopCharacteristics.getCollectionString().trim()).isEqualTo(PARTICIPANT_VALUE);
                    assertThat(loopCharacteristics.getHandler())
                            .extracting(CollectionHandler::getImplementationType, CollectionHandler::getImplementation)
                            .containsExactly("class", "org.flowable.engine.test.bpmn.multiinstance.JSONCollectionHandler");
                });

        // verify subprocess
        flowElement = main.getFlowElement("subprocess1");
        assertThat(flowElement)
                .isInstanceOfSatisfying(SubProcess.class, subProcess -> {
                    assertThat(subProcess.getName()).isEqualTo("subProcess");
                    MultiInstanceLoopCharacteristics loopCharacteristics = subProcess.getLoopCharacteristics();
                    assertThat(loopCharacteristics.isSequential()).isTrue();
                    assertThat(loopCharacteristics.getLoopCardinality()).isEqualTo("10");
                    assertThat(subProcess.getFlowElements()).hasSize(5);

                    // verify user task in subprocess
                    FlowElement task = subProcess.getFlowElement("subUserTask1");
                    assertThat(task)
                            .isInstanceOfSatisfying(UserTask.class, userTask -> {
                                assertThat(userTask.getName()).isEqualTo("User task 2");
                                MultiInstanceLoopCharacteristics loopCharacteristics2 = userTask.getLoopCharacteristics();
                                assertThat(loopCharacteristics2.isSequential()).isTrue();
                                assertThat(loopCharacteristics2.getElementVariable()).isEqualTo("participant");
                                assertThat(loopCharacteristics2.getInputDataItem()).isEqualTo("${participants}");
                                assertThat(loopCharacteristics2.getCompletionCondition()).isEqualTo("${numActiveTasks == \"2\"}");
                            });
                });
    }

    @BpmnXmlConverterTest("multiInstanceVariableAggregationsModel.bpmn")
    void variableAggregations(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess()
                .getFlowElement("miTasks");

        assertThat(flowElement).isInstanceOf(UserTask.class);
        UserTask userTask = (UserTask) flowElement;
        MultiInstanceLoopCharacteristics loopCharacteristics = userTask.getLoopCharacteristics();
        assertThat(loopCharacteristics).isNotNull();
        assertThat(loopCharacteristics.getAggregations()).isNotNull();
        List<VariableAggregationDefinition> aggregations = new ArrayList<>(loopCharacteristics.getAggregations().getAggregations());
        assertThat(aggregations)
                .extracting(VariableAggregationDefinition::getTarget, VariableAggregationDefinition::getTargetExpression,
                        VariableAggregationDefinition::getImplementationType, VariableAggregationDefinition::getImplementation)
                .containsExactly(
                        tuple("reviews", null, null, null),
                        tuple(null, "${targetVar}", null, null),
                        tuple("reviews", null, ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION, "${customVariableAggregator}"),
                        tuple("reviews", null, ImplementationType.IMPLEMENTATION_TYPE_CLASS, "com.example.flowable.CustomVariableAggregator")
                );

        assertThat(aggregations.get(0).getDefinitions())
                .extracting(VariableAggregationDefinition.Variable::getSource, VariableAggregationDefinition.Variable::getSourceExpression,
                        VariableAggregationDefinition.Variable::getTarget, VariableAggregationDefinition.Variable::getTargetExpression)
                .containsExactly(
                        tuple("taskAssignee", null, "userId", null),
                        tuple("approved", null, null, null),
                        tuple(null, "${score * 2}", null, "${targetVar}")
                );

        assertThat(aggregations.get(1).getDefinitions())
                .extracting(VariableAggregationDefinition.Variable::getSource, VariableAggregationDefinition.Variable::getSourceExpression,
                        VariableAggregationDefinition.Variable::getTarget, VariableAggregationDefinition.Variable::getTargetExpression)
                .containsExactly(
                        tuple("taskAssignee", null, "userId", null),
                        tuple("approved", null, null, null)
                );

        assertThat(aggregations.get(2).getDefinitions())
                .extracting(VariableAggregationDefinition.Variable::getSource, VariableAggregationDefinition.Variable::getSourceExpression,
                        VariableAggregationDefinition.Variable::getTarget, VariableAggregationDefinition.Variable::getTargetExpression)
                .containsExactly(
                        tuple("approved", null, null, null)
                );

        assertThat(aggregations.get(3).getDefinitions())
                .extracting(VariableAggregationDefinition.Variable::getSource, VariableAggregationDefinition.Variable::getSourceExpression,
                        VariableAggregationDefinition.Variable::getTarget, VariableAggregationDefinition.Variable::getTargetExpression)
                .containsExactly(
                        tuple("description", null, null, null)
                );
    }
}
