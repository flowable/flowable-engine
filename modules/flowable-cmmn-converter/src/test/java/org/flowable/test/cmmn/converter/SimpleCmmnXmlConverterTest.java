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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Milestone;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryOnPart;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.Task;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class SimpleCmmnXmlConverterTest extends AbstractConverterTest {

    private static final String CMMN_RESOURCE = "org/flowable/test/cmmn/converter/simple.cmmn";

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
        assertEquals("test", caze.getInitiatorVariableName());
        assertEquals(2, caze.getCandidateStarterUsers().size());
        assertTrue(caze.getCandidateStarterUsers().contains("test"));
        assertTrue(caze.getCandidateStarterUsers().contains("test2"));
        assertEquals(2, caze.getCandidateStarterGroups().size());
        assertTrue(caze.getCandidateStarterGroups().contains("group"));
        assertTrue(caze.getCandidateStarterGroups().contains("group2"));

        // Plan model
        Stage planModel = caze.getPlanModel();
        assertNotNull(planModel);
        assertEquals("myPlanModel", planModel.getId());
        assertEquals("My CasePlanModel", planModel.getName());
        assertEquals("formKey", planModel.getFormKey());

        // Sentries
        assertEquals(3, planModel.getSentries().size());
        for (Sentry sentry : planModel.getSentries()) {
            List<SentryOnPart> onParts = sentry.getOnParts();
            assertEquals(1, onParts.size());
            assertNotNull(onParts.get(0).getId());
            assertNotNull(onParts.get(0).getSourceRef());
            assertNotNull(onParts.get(0).getSource());
            assertNotNull(onParts.get(0).getStandardEvent());
        }

        // Plan items definitions
        List<PlanItemDefinition> planItemDefinitions = planModel.getPlanItemDefinitions();
        assertEquals(4, planItemDefinitions.size());
        assertEquals(2, planModel.findPlanItemDefinitionsOfType(Task.class, false).size());
        assertEquals(2, planModel.findPlanItemDefinitionsOfType(Milestone.class, false).size());
        for (PlanItemDefinition planItemDefinition : planItemDefinitions) {
            assertNotNull(planItemDefinition.getId());
            assertNotNull(planItemDefinition.getName());
        }

        // Plan items
        List<PlanItem> planItems = planModel.getPlanItems();
        assertEquals(4, planItems.size());
        int nrOfTasks = 0;
        int nrOfMileStones = 0;
        for (PlanItem planItem : planItems) {
            assertNotNull(planItem.getId());
            assertNotNull(planItem.getDefinitionRef());
            assertNotNull(planItem.getPlanItemDefinition()); // Verify plan item definition ref is resolved

            PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
            if (planItemDefinition instanceof Milestone) {
                nrOfMileStones++;
            } else if (planItemDefinition instanceof Task) {
                nrOfTasks++;
            }

            if (!planItem.getId().equals("planItemTaskA")) {
                assertNotNull(planItem.getEntryCriteria());
                assertEquals(1, planItem.getEntryCriteria().size());
                assertNotNull(planItem.getEntryCriteria().get(0).getSentry()); // Verify if sentry reference is resolved
            }

            if (planItem.getPlanItemDefinition() instanceof Task) {
                if (planItem.getId().equals("planItemTaskB")) {
                    assertFalse(((Task) planItem.getPlanItemDefinition()).isBlocking());
                } else {
                    assertTrue(((Task) planItem.getPlanItemDefinition()).isBlocking());
                }
            }
        }

        assertEquals(2, nrOfMileStones);
        assertEquals(2, nrOfTasks);
    }

}
