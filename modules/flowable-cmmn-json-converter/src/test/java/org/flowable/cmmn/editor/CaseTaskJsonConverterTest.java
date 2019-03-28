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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;

/**
 * @author martin.grofcik
 */
public class CaseTaskJsonConverterTest extends AbstractConverterTest {
    @Override
    protected String getResource() {
        return "test.caseTaskModel.json";
    }

    @Override
    protected void validateModel(CmmnModel model) {
        Case caseModel = model.getPrimaryCase();
        assertEquals("caseTaskModelId", caseModel.getId());
        assertEquals("caseTaskModelName", caseModel.getName());

        Stage planModelStage = caseModel.getPlanModel();
        assertNotNull(planModelStage);
        assertEquals("casePlanModel", planModelStage.getId());

        PlanItem planItem = planModelStage.findPlanItemInPlanFragmentOrUpwards("planItem1");
        assertNotNull(planItem);
        assertEquals("planItem1", planItem.getId());
        assertEquals("caseTaskName", planItem.getName());
        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
        assertNotNull(planItemDefinition);
        assertTrue(planItemDefinition instanceof CaseTask);
        CaseTask caseTask = (CaseTask) planItemDefinition;
        assertEquals("sid-E06221FA-0225-4EF8-A1E8-8DC177326B77", caseTask.getId());
        assertEquals("caseTaskName", caseTask.getName());
        assertTrue(caseTask.getFallbackToDefaultTenant());
    }
}
