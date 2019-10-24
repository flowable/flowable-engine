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

import java.util.List;

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ParentCompletionRule;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Stage;
import org.junit.Test;

public class ParentCompletionConverterTest extends AbstractConverterTest {
    
    private static final String CMMN_RESOURCE = "org/flowable/test/cmmn/converter/parentCompletionRuleAtPlanItem.cmmn";
    
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
        Stage planModel = cmmnModel.getPrimaryCase().getPlanModel();
        List<PlanItem> planItems = planModel.getPlanItems();
        assertEquals(4, planItems.size());
        planItems.forEach(planItem -> {
            assertNotNull(planItem.getItemControl());
            assertNotNull(planItem.getItemControl().getParentCompletionRule());
            assertNotNull(planItem.getItemControl().getParentCompletionRule().getType());
        });
        
        assertEquals(ParentCompletionRule.ALWAYS_IGNORE, planItems.get(0).getItemControl().getParentCompletionRule().getType());
        assertEquals(ParentCompletionRule.DEFAULT, planItems.get(1).getItemControl().getParentCompletionRule().getType());
        assertEquals(ParentCompletionRule.IGNORE_IF_AVAILABLE, planItems.get(2).getItemControl().getParentCompletionRule().getType());
        assertEquals(ParentCompletionRule.IGNORE_IF_AVAILABLE_OR_ENABLED, planItems.get(3).getItemControl().getParentCompletionRule().getType());

        Stage stageOne = (Stage) cmmnModel.getPrimaryCase().getPlanModel().findPlanItemDefinitionInStageOrDownwards("stageOne");
        List<PlanItem> planItems1 = stageOne.getPlanItems();
        assertEquals(1, planItems1.size());
        PlanItem planItem = planItems1.get(0);
        assertNotNull(planItem.getItemControl());
        assertNotNull(planItem.getItemControl().getParentCompletionRule());
        assertEquals(ParentCompletionRule.ALWAYS_IGNORE_AFTER_FIRST_COMPLETION, planItem.getItemControl().getParentCompletionRule().getType());
    }

}
