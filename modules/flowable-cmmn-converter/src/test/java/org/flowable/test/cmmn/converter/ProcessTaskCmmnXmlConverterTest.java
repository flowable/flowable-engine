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
package org.flowable.test.cmmn.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.IOParameter;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.ProcessTask;
import org.flowable.cmmn.model.Stage;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class ProcessTaskCmmnXmlConverterTest extends AbstractConverterTest {
    
    private static final String CMMN_RESOURCE = "org/flowable/test/cmmn/converter/process-task.cmmn";
    
    @Test
    public void convertXMLToModel() throws Exception {
        CmmnModel cmmnModel = readXMLFile(CMMN_RESOURCE);
        validateModel(cmmnModel);
    }

    @Test
    public void convertModelToXML() throws Exception {
        CmmnModel cmmnModel = readXMLFile(CMMN_RESOURCE);
        CmmnModel parsedModel = exportAndReadXMLFile(cmmnModel);
        validateModel(parsedModel);
    }
    
    public void validateModel(CmmnModel cmmnModel) {
        assertNotNull(cmmnModel);
        assertEquals(1, cmmnModel.getCases().size());
        
        // Case
        Case caze = cmmnModel.getCases().get(0);
        assertEquals("myCase", caze.getId());
        
        // Plan model
        Stage planModel = caze.getPlanModel();
        assertNotNull(planModel);
        assertEquals("myPlanModel", planModel.getId());
        assertEquals("My CasePlanModel", planModel.getName());
        
        PlanItem planItemTask1 = cmmnModel.findPlanItem("planItem1");
        PlanItemDefinition planItemDefinition = planItemTask1.getPlanItemDefinition();
        assertTrue(planItemDefinition instanceof ProcessTask);
        ProcessTask task1 = (ProcessTask) planItemDefinition;
        assertEquals("${processDefinitionKey}", task1.getProcessRefExpression());
        
        assertEquals(1, task1.getInParameters().size());
        IOParameter parameter = task1.getInParameters().get(0);
        assertEquals("num2", parameter.getSource());
        assertEquals("num", parameter.getTarget());
        
        assertEquals(1, task1.getOutParameters().size());
        parameter = task1.getOutParameters().get(0);
        assertEquals("num", parameter.getSource());
        assertEquals("num3", parameter.getTarget());
    }

}
