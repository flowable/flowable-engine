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
package org.flowable.editor.language;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.bpmn.model.Association;
import org.flowable.bpmn.model.AssociationDirection;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ScriptTask;
import org.junit.jupiter.api.Test;

public class BoundaryEventAssociationConverterTest extends AbstractConverterTest {

    @Test
    public void convertJsonToModel() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        validateModel(bpmnModel);
    }

    @Override
    protected String getResource() {
        return "test.boundaryeventassociationmodel.json";
    }

    private void validateModel(BpmnModel model) {

        BoundaryEvent compensateElement1 = (BoundaryEvent) model.getMainProcess().getFlowElement("COMP_1", true);
        assertThat(compensateElement1.isCancelActivity()).isFalse();
        assertThat(compensateElement1.getAttachedToRefId()).isEqualTo("SCRIPT_1");
        Association comp1ToCompscript11 = (Association) model.getMainProcess().getArtifact("COMP1_TO_COMPSCRIPT11");
        assertThat(comp1ToCompscript11.getSourceRef()).isEqualTo(compensateElement1.getId());
        assertThat(comp1ToCompscript11.getTargetRef()).isEqualTo("COMP_SCRIPT_11");
        assertThat(comp1ToCompscript11.getAssociationDirection()).isEqualTo(AssociationDirection.NONE);

        BoundaryEvent compensateElement2 = (BoundaryEvent) model.getMainProcess().getFlowElement("COMP_2", true);
        assertThat(compensateElement2.isCancelActivity()).isFalse();
        assertThat(compensateElement2.getAttachedToRefId()).isEqualTo("SCRIPT_2");
        Association comp1ToCompscript21 = (Association) model.getMainProcess().getArtifact("COMP2_TO_COMPSCRIPT21");
        assertThat(comp1ToCompscript21.getSourceRef()).isEqualTo(compensateElement2.getId());
        assertThat(comp1ToCompscript21.getTargetRef()).isEqualTo("COMP_SCRIPT_21");
        assertThat(comp1ToCompscript21.getAssociationDirection()).isEqualTo(AssociationDirection.NONE);

        BoundaryEvent compensateElement3 = (BoundaryEvent) model.getMainProcess().getFlowElement("COMP_3", true);
        assertThat(compensateElement2.isCancelActivity()).isFalse();
        assertThat(compensateElement3.getAttachedToRefId()).isEqualTo("SCRIPT_3");
        Association comp1ToCompscript31 = (Association) model.getMainProcess().getArtifact("COMP3_TO_COMPSCRIPT31");
        assertThat(comp1ToCompscript31.getSourceRef()).isEqualTo(compensateElement3.getId());
        assertThat(comp1ToCompscript31.getTargetRef()).isEqualTo("COMP_SCRIPT_31");
        assertThat(comp1ToCompscript31.getAssociationDirection()).isEqualTo(AssociationDirection.NONE);

        ScriptTask scriptTask1 = (ScriptTask) model.getMainProcess().getFlowElement("SCRIPT_1", true);
        assertThat(scriptTask1.isForCompensation()).isFalse();
        ScriptTask scriptTask2 = (ScriptTask) model.getMainProcess().getFlowElement("SCRIPT_2", true);
        assertThat(scriptTask2.isForCompensation()).isFalse();
        ScriptTask scriptTask3 = (ScriptTask) model.getMainProcess().getFlowElement("SCRIPT_3", true);
        assertThat(scriptTask3.isForCompensation()).isFalse();

        ScriptTask compScript11 = (ScriptTask) model.getMainProcess().getFlowElement("COMP_SCRIPT_11", true);
        assertThat(compScript11.isForCompensation()).isTrue();
        ScriptTask compScript21 = (ScriptTask) model.getMainProcess().getFlowElement("COMP_SCRIPT_21", true);
        assertThat(compScript21.isForCompensation()).isTrue();
        ScriptTask compScript31 = (ScriptTask) model.getMainProcess().getFlowElement("COMP_SCRIPT_31", true);
        assertThat(compScript31.isForCompensation()).isTrue();

    }

}
