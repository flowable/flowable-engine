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
package org.flowable.cmmn.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanFragment;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Stage;

/**
 * @author Joram Barrez
 */
public class PlanItemControlConverterTest extends AbstractConverterTest {
    
    @Override
    protected String getResource() {
        return "test.planItemControl.json";
    }

    @Override
    protected void validateModel(CmmnModel cmmnModel) {
        
        // The plan model should have auto complete enabled
        Stage planModel = cmmnModel.getPrimaryCase().getPlanModel();
        assertTrue(planModel.isAutoComplete());
        
        // Stage A should not have auto complete disabled
        Stage stageA = findStageByName(planModel, "Stage A");
        assertFalse(stageA.isAutoComplete());
        assertNull(stageA.getAutoCompleteCondition());
        
        // Stage A, Task A, Task B and Task D should not have any item control set
        assertNoItemControlsSet(cmmnModel, "Stage A");
        assertNoItemControlsSet(cmmnModel, "A");
        assertNoItemControlsSet(cmmnModel, "B");
        assertNoItemControlsSet(cmmnModel, "D");
        
        // Stage B should have everything enabled
        Stage stageB = findStageByName(planModel, "Stage B");
        assertTrue(stageB.isAutoComplete());
        assertNull(stageB.getAutoCompleteCondition());
        
        PlanItem stageBPlanItem = findPlanItemByName(planModel, "Stage B");
        assertNotNull(stageBPlanItem.getItemControl().getRequiredRule());
        assertNull(stageBPlanItem.getItemControl().getRequiredRule().getCondition());
        assertNotNull(stageBPlanItem.getItemControl().getManualActivationRule());
        assertNull(stageBPlanItem.getItemControl().getManualActivationRule().getCondition());
        assertNotNull(stageBPlanItem.getItemControl().getRepetitionRule());
        assertNull(stageBPlanItem.getItemControl().getRepetitionRule().getCondition());
        
        // Stage C should have an autocomplete condition set
        Stage stageC = findStageByName(planModel, "Stage C");
        assertTrue(stageC.isAutoComplete());
        assertEquals("${someVariable}", stageC.getAutoCompleteCondition());
        assertNoItemControlsSet(cmmnModel, "Stage C");
        
        // Task C should have all item controls enabled
        assertAllItemControlsSet(cmmnModel, "C");
        
        PlanItem taskCPlanItem = findPlanItemByName(planModel, "C");
        assertNull(taskCPlanItem.getItemControl().getRequiredRule().getCondition());
        assertNull(taskCPlanItem.getItemControl().getRepetitionRule().getCondition());
        assertNull(taskCPlanItem.getItemControl().getManualActivationRule().getCondition());
        
        // Task E should have a condition set for all item controls
        assertAllItemControlsSet(cmmnModel, "C");
        
        PlanItem taskEPlanItem = findPlanItemByName(planModel, "E");
        assertEquals("${requiredCondition}", taskEPlanItem.getItemControl().getRequiredRule().getCondition());
        assertEquals("${repetitionCondition}", taskEPlanItem.getItemControl().getRepetitionRule().getCondition());
        assertEquals("${manualActivationCondition}", taskEPlanItem.getItemControl().getManualActivationRule().getCondition());
    }

    protected void assertNoItemControlsSet(CmmnModel cmmnModel, String planItemName) {
        PlanItem planItem = findPlanItemByName(cmmnModel.getPrimaryCase().getPlanModel(),planItemName);
        assertNotNull("No plan item found with name " + planItemName, planItem);
        if (planItem.getItemControl() != null) {
            assertNull(planItem.getItemControl().getRequiredRule());
            assertNull(planItem.getItemControl().getRepetitionRule());
            assertNull(planItem.getItemControl().getManualActivationRule());
        } else {
            assertNull(planItem.getItemControl());
        }
    }
    
    protected void assertAllItemControlsSet(CmmnModel cmmnModel, String planItemName) {
        PlanItem planItem = findPlanItemByName(cmmnModel.getPrimaryCase().getPlanModel(),planItemName);
        assertNotNull("No plan item found with name " + planItemName, planItem);
        assertNotNull(planItem.getItemControl().getRequiredRule());
        assertNotNull(planItem.getItemControl().getRepetitionRule());
        assertNotNull(planItem.getItemControl().getManualActivationRule());
    }
    
    protected Stage findStageByName(Stage planModel, String stageName) {
        List<Stage> stages = planModel.findPlanItemDefinitionsOfType(Stage.class, true);
        for (Stage stage : stages) {
            if (stageName.equals(stage.getName())) {
                return stage;
            }
        }
        fail("No stage found with name " + stageName);
        return null;
    }
    
    protected PlanItem findPlanItemByName(PlanFragment planFragment, String planItemName) {
        List<PlanItem> planItems = planFragment.getPlanItems();
        if (planItems != null) {
            for (PlanItem planItem : planItems) {
                
                if (planItemName.equals(planItem.getName())) {
                    return planItem;
                }
                
                if (planItem.getPlanItemDefinition() instanceof PlanFragment) {
                    PlanItem p = findPlanItemByName((PlanFragment) planItem.getPlanItemDefinition(), planItemName);
                    if (p != null) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

}
