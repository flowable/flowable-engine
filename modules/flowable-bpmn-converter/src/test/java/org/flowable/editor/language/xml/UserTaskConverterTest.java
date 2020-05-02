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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.FormProperty;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.UserTask;
import org.junit.Test;

public class UserTaskConverterTest extends AbstractConverterTest {

    @Test
    public void convertXMLToModel() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        validateModel(bpmnModel);
    }

    @Test
    public void convertModelToXML() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        BpmnModel parsedModel = exportAndReadXMLFile(bpmnModel);
        validateModel(parsedModel);
    }

    @Override
    protected String getResource() {
        return "usertaskmodel.bpmn";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("usertask");
        assertThat(flowElement)
                .isInstanceOfSatisfying(UserTask.class, userTask -> {
                    assertThat(userTask.getId()).isEqualTo("usertask");
                    assertThat(userTask.getName()).isEqualTo("User task");
                    assertThat(userTask.getCategory()).isEqualTo("Test Category");
                    assertThat(userTask.getFormKey()).isEqualTo("testKey");
                    assertThat(userTask.isSameDeployment()).isTrue();
                    assertThat(userTask.getPriority()).isEqualTo("40");
                    assertThat(userTask.getDueDate()).isEqualTo("2012-11-01");

                    assertThat(userTask.getBusinessCalendarName()).isEqualTo("customCalendarName");

                    assertThat(userTask.getAssignee()).isEqualTo("kermit");
                    assertThat(userTask.getCandidateUsers()).hasSize(2);
                    assertThat(userTask.getCandidateUsers().contains("kermit")).isTrue();
                    assertThat(userTask.getCandidateUsers().contains("fozzie")).isTrue();
                    assertThat(userTask.getCandidateGroups()).hasSize(2);
                    assertThat(userTask.getCandidateGroups().contains("management")).isTrue();
                    assertThat(userTask.getCandidateGroups().contains("sales")).isTrue();

                    assertThat(userTask.getCustomUserIdentityLinks()).hasSize(1);
                    assertThat(userTask.getCustomGroupIdentityLinks()).hasSize(2);
                    assertThat(userTask.getCustomUserIdentityLinks().get("businessAdministrator").contains("kermit")).isTrue();
                    assertThat(userTask.getCustomGroupIdentityLinks().get("manager").contains("management")).isTrue();
                    assertThat(userTask.getCustomGroupIdentityLinks().get("businessAdministrator").contains("management")).isTrue();

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
    }
}
