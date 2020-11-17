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

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class ScopedConverterTest {

    @BpmnXmlConverterTest("scopedmodel.bpmn")
    void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("outerSubProcess");
        assertThat(flowElement)
                .isInstanceOfSatisfying(SubProcess.class, outerProcess -> {
                    assertThat(outerProcess.getId()).isEqualTo("outerSubProcess");
                    assertThat(outerProcess.getBoundaryEvents())
                            .extracting(BoundaryEvent::getId)
                            .containsExactly("outerBoundaryEvent");
                    assertThat(outerProcess.getFlowElement("innerSubProcess"))
                            .isInstanceOfSatisfying(SubProcess.class, innerProcess -> {
                                assertThat(innerProcess.getId()).isEqualTo("innerSubProcess");
                                assertThat(innerProcess.getBoundaryEvents())
                                        .extracting(BoundaryEvent::getId)
                                        .containsExactly("innerBoundaryEvent");
                                assertThat(innerProcess.getFlowElement("usertask"))
                                        .isInstanceOfSatisfying(UserTask.class, userTask -> {
                                            assertThat(userTask.getBoundaryEvents())
                                                    .extracting(BoundaryEvent::getId)
                                                    .containsExactly("taskBoundaryEvent");
                                        });
                            });
                });
    }
}
