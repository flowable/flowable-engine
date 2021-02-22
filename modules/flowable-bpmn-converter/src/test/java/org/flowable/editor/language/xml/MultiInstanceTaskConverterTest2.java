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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CollectionHandler;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

/**
 * @see <a href="https://github.com/flowable/flowable-engine/issues/474">Issue 474</a>
 */
class MultiInstanceTaskConverterTest2 {
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

    @BpmnXmlConverterTest("multiinstancemodel2.bpmn")
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
                .isInstanceOfSatisfying(UserTask.class, task -> {
                    assertThat(task.getName()).isEqualTo("User task 1");
                    MultiInstanceLoopCharacteristics loopCharacteristics = task.getLoopCharacteristics();
                    assertThat(loopCharacteristics.getElementVariable()).isEqualTo("participant");
                    assertThat(loopCharacteristics.getCollectionString().trim()).isEqualTo(PARTICIPANT_VALUE);
                    assertThat(loopCharacteristics.getHandler())
                            .extracting(CollectionHandler::getImplementationType, CollectionHandler::getImplementation)
                            .containsExactly("delegateExpression", "${collectionHandler}");
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
                                assertThat(loopCharacteristics2)
                                        .extracting(MultiInstanceLoopCharacteristics::getElementVariable, MultiInstanceLoopCharacteristics::getInputDataItem)
                                        .containsExactly("participant", "${potentialOwnerList}");
                                assertThat(loopCharacteristics2.getHandler())
                                        .extracting(CollectionHandler::getImplementationType, CollectionHandler::getImplementation)
                                        .containsExactly("delegateExpression", "${collectionHandler}");
                            });
                });
    }
}