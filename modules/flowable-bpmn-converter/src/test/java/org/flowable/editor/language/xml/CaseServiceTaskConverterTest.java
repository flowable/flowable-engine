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

import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CaseServiceTask;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IOParameter;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class CaseServiceTaskConverterTest {

    @BpmnXmlConverterTest("caseservicetask.bpmn")
    void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("caseServiceTask");
        assertThat(flowElement).isInstanceOf(CaseServiceTask.class);
        CaseServiceTask caseServiceTask = (CaseServiceTask) flowElement;
        assertThat(caseServiceTask.getId()).isEqualTo("caseServiceTask");
        assertThat(caseServiceTask.getName()).isEqualTo("Case task");

        assertThat(caseServiceTask.getCaseDefinitionKey()).isEqualTo("caseId");

        assertThat(caseServiceTask.isFallbackToDefaultTenant()).isTrue();
        assertThat(caseServiceTask.isSameDeployment()).isFalse();

        List<IOParameter> parameters = caseServiceTask.getInParameters();
        assertThat(parameters)
                .extracting(IOParameter::getSource, IOParameter::getTarget, IOParameter::getSourceExpression)
                .containsExactly(
                        tuple("test", "test", null),
                        tuple(null, "test", "${test}")
                );

        parameters = caseServiceTask.getOutParameters();
        assertThat(parameters)
                .extracting(IOParameter::getSource, IOParameter::getTarget)
                .containsExactly(
                        tuple("test", "test")
                );
    }
}
