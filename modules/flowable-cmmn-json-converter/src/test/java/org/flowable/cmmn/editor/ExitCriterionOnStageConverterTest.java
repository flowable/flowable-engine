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
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;

public class ExitCriterionOnStageConverterTest extends AbstractConverterTest {

    @Override
    protected String getResource() {
        return "test.exitCriterionOnStageModel.json";
    }

    @Override
    protected void validateModel(CmmnModel model) {
        Case caseModel = model.getPrimaryCase();
        Stage planModelStage = caseModel.getPlanModel();
        PlanItemDefinition planItemDefinition = planModelStage.findPlanItemDefinitionInStageOrDownwards("sid-46EAD2FE-4D89-42ED-9B1E-5005AE5BF2F7");
        assertTrue(planItemDefinition instanceof Stage);

        PlanItem planItem = planModelStage.findPlanItemInPlanFragmentOrDownwards(planItemDefinition.getPlanItemRef());
        assertEquals(1, planItem.getEntryCriteria().size());
        Criterion entryCriterion = planItem.getEntryCriteria().get(0);
        assertNotNull(entryCriterion.getSentryRef());

        assertEquals(1, planItem.getExitCriteria().size());
        Criterion exitCriterion = planItem.getExitCriteria().get(0);
        assertNotNull(exitCriterion.getSentryRef());
    }
}
