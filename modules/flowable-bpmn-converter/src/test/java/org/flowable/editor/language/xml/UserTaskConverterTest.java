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
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Collections;

import org.flowable.bpmn.model.BpmnDiEdge;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.FormProperty;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.UserTask;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class UserTaskConverterTest {

    @BpmnXmlConverterTest("usertaskmodel.bpmn")
    void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("usertask");
        assertThat(flowElement)
                .isInstanceOfSatisfying(UserTask.class, userTask -> {
                    assertThat(userTask.getId()).isEqualTo("usertask");
                    assertThat(userTask.getName()).isEqualTo("User task");
                    assertThat(userTask.getCategory()).isEqualTo("Test Category");
                    assertThat(userTask.getFormKey()).isEqualTo("testKey");
                    assertThat(userTask.isSameDeployment()).isTrue();
                    assertThat(userTask.getPriority()).isEqualTo("40");
                    assertThat(userTask.getTaskIdVariableName()).isEqualTo("myTaskId");
                    assertThat(userTask.getDueDate()).isEqualTo("2012-11-01");

                    assertThat(userTask.getBusinessCalendarName()).isEqualTo("customCalendarName");

                    assertThat(userTask.getAssignee()).isEqualTo("kermit");
                    assertThat(userTask.getCandidateUsers()).containsExactlyInAnyOrder("kermit", "fozzie");
                    assertThat(userTask.getCandidateGroups()).containsExactlyInAnyOrder("management", "sales");

                    assertThat(userTask.getCustomUserIdentityLinks()).hasSize(1);
                    assertThat(userTask.getCustomGroupIdentityLinks()).hasSize(2);
                    assertThat(userTask.getCustomUserIdentityLinks())
                            .containsOnly(entry("businessAdministrator", Collections.singleton("kermit")));
                    assertThat(userTask.getCustomGroupIdentityLinks())
                            .containsOnly(
                                    entry("manager", Collections.singleton("management")),
                                    entry("businessAdministrator", Collections.singleton("management"))
                            );

                    assertThat(userTask.getFormProperties())
                            .extracting(FormProperty::getId, FormProperty::getName, FormProperty::getType, FormProperty::getVariable,
                                    FormProperty::getExpression)
                            .containsExactly(
                                    tuple("formId", "formName", "string", "variable", "${expression}"),
                                    tuple("formId2", "anotherName", "long", null, null),
                                    tuple("formId3", "enumName", "enum", null, null)
                            );
                    assertThat(userTask.getFormProperties().get(2).getFormValues()).hasSize(2);

                    assertThat(userTask.getTaskListeners())
                            .extracting(FlowableListener::getImplementationType, FlowableListener::getImplementation, FlowableListener::getEvent,
                                    FlowableListener::getOnTransaction, FlowableListener::getCustomPropertiesResolverImplementation)
                            .containsExactly(
                                    tuple(ImplementationType.IMPLEMENTATION_TYPE_CLASS, "org.test.TestClass", "create", "before-commit",
                                            "org.test.TestResolverClass"),
                                    tuple(ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION, "${someExpression}", "assignment", "committed",
                                            "${testResolverExpression}"),
                                    tuple(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION, "${someDelegateExpression}", "complete", "rolled-back",
                                            "${delegateResolverExpression}")
                            );

                    assertThat(userTask.getExecutionListeners())
                            .extracting(FlowableListener::getEvent, FlowableListener::getOnTransaction,
                                    FlowableListener::getCustomPropertiesResolverImplementation)
                            .containsExactly(tuple("end", "before-commit", "org.test.TestResolverClass"));
                });
        
        assertThat(model.getEdgeInfo("flow2")).isNotNull();
        BpmnDiEdge edgeInfo = model.getEdgeInfo("flow2");
        assertThat(edgeInfo.getSourceDockerInfo().getX()).isEqualTo(50.0);
        assertThat(edgeInfo.getSourceDockerInfo().getY()).isEqualTo(10.0);
        assertThat(edgeInfo.getTargetDockerInfo().getX()).isEqualTo(40.0);
        assertThat(edgeInfo.getTargetDockerInfo().getY()).isEqualTo(30.0);
        assertThat(edgeInfo.getWaypoints()).hasSize(2);
        
        assertThat(model.getEdgeInfo("flow1")).isNull();
    }
}
