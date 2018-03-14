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
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.ScriptServiceTask;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.Task;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class ScriptServiceTaskCmmnXmlConverterTest extends AbstractConverterTest {

    private static final String CMMN_RESOURCE = "org/flowable/test/cmmn/converter/script-task.cmmn";

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
        assertEquals("scriptCase", caze.getId());
        assertEquals("test", caze.getInitiatorVariableName());

        // Plan model
        Stage planModel = caze.getPlanModel();
        assertNotNull(planModel);
        assertEquals("myScriptPlanModel", planModel.getId());
        assertEquals("My Script CasePlanModel", planModel.getName());

        // Plan items definitions
        List<PlanItemDefinition> planItemDefinitions = planModel.getPlanItemDefinitions();
        assertEquals(1, planItemDefinitions.size());
        assertEquals(1, planModel.findPlanItemDefinitionsOfType(Task.class, false).size());

        // Plan items
        List<PlanItem> planItems = planModel.getPlanItems();
        assertEquals(1, planItems.size());

        PlanItem planItemTaskA = cmmnModel.findPlanItem("planItemTaskA");
        PlanItemDefinition planItemDefinition = planItemTaskA.getPlanItemDefinition();
        assertEquals(0, planItemTaskA.getEntryCriteria().size());
        assertTrue(planItemDefinition instanceof ScriptServiceTask);
        ScriptServiceTask scriptTask = (ScriptServiceTask) planItemDefinition;
        assertEquals(ScriptServiceTask.SCRIPT_TASK, scriptTask.getType());
        assertEquals("javascript", scriptTask.getScriptFormat());
        assertEquals("scriptResult", scriptTask.getResultVariableName());
        assertFalse(scriptTask.isAutoStoreVariables());
        assertTrue(scriptTask.isBlocking());
        assertFalse(scriptTask.isAsync());
        
        assertEquals(1, scriptTask.getFieldExtensions().size());
        FieldExtension fieldExtension = scriptTask.getFieldExtensions().get(0);
        assertEquals("script", fieldExtension.getFieldName());
        assertEquals("var a = 5;", fieldExtension.getStringValue());
    }

}
