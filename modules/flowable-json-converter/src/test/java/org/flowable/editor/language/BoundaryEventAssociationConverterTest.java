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

import org.flowable.bpmn.model.*;
import org.junit.Test;


import static org.junit.Assert.*;

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
        assertFalse(compensateElement1.isCancelActivity());
        assertEquals("SCRIPT_1", compensateElement1.getAttachedToRefId());
        Association comp1ToCompscript11 = (Association) model.getMainProcess().getArtifact("COMP1_TO_COMPSCRIPT11");
        assertEquals(compensateElement1.getId(),comp1ToCompscript11.getSourceRef());
        assertEquals("COMP_SCRIPT_11",comp1ToCompscript11.getTargetRef());
        assertEquals(AssociationDirection.NONE,comp1ToCompscript11.getAssociationDirection());


        BoundaryEvent compensateElement2 = (BoundaryEvent) model.getMainProcess().getFlowElement("COMP_2", true);
        assertFalse(compensateElement2.isCancelActivity());
        assertEquals("SCRIPT_2", compensateElement2.getAttachedToRefId());
        Association comp1ToCompscript21 = (Association) model.getMainProcess().getArtifact("COMP2_TO_COMPSCRIPT21");
        assertEquals(compensateElement2.getId(),comp1ToCompscript21.getSourceRef());
        assertEquals("COMP_SCRIPT_21",comp1ToCompscript21.getTargetRef());
        assertEquals(AssociationDirection.NONE,comp1ToCompscript21.getAssociationDirection());

        BoundaryEvent compensateElement3 = (BoundaryEvent) model.getMainProcess().getFlowElement("COMP_3", true);
        assertFalse(compensateElement2.isCancelActivity());
        assertEquals("SCRIPT_3", compensateElement3.getAttachedToRefId());
        Association comp1ToCompscript31 = (Association) model.getMainProcess().getArtifact("COMP3_TO_COMPSCRIPT31");
        assertEquals(compensateElement3.getId(),comp1ToCompscript31.getSourceRef());
        assertEquals("COMP_SCRIPT_31",comp1ToCompscript31.getTargetRef());
        assertEquals(AssociationDirection.NONE,comp1ToCompscript31.getAssociationDirection());


        ScriptTask scriptTask1 = (ScriptTask) model.getMainProcess().getFlowElement("SCRIPT_1",true);
        assertFalse(scriptTask1.isForCompensation());
        ScriptTask scriptTask2 = (ScriptTask) model.getMainProcess().getFlowElement("SCRIPT_2",true);
        assertFalse(scriptTask2.isForCompensation());
        ScriptTask scriptTask3 = (ScriptTask) model.getMainProcess().getFlowElement("SCRIPT_3",true);
        assertFalse(scriptTask3.isForCompensation());


        ScriptTask compScript11 = (ScriptTask) model.getMainProcess().getFlowElement("COMP_SCRIPT_11",true);
        assertTrue(compScript11.isForCompensation());
        ScriptTask compScript21 = (ScriptTask) model.getMainProcess().getFlowElement("COMP_SCRIPT_21",true);
        assertTrue(compScript21.isForCompensation());
        ScriptTask compScript31 = (ScriptTask) model.getMainProcess().getFlowElement("COMP_SCRIPT_31",true);
        assertTrue(compScript31.isForCompensation());

    }

}
