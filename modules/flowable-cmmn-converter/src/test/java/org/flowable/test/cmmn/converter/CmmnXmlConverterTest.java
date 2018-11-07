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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.flowable.cmmn.converter.CmmnXmlConverter;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.Milestone;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryOnPart;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.Task;
import org.flowable.common.engine.api.io.InputStreamProvider;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class CmmnXmlConverterTest {

    private CmmnXmlConverter cmmnXmlConverter;

    @Before
    public void setup() {
        this.cmmnXmlConverter = new CmmnXmlConverter();
    }

    /**
     * Test simple case model, with 4 consequent elements: taskA -> milestone 1 -> taskB -> milestone 2.
     *
     * The converters should check following model class instances:
     * - 1 case
     * - 1 stage (the plan model)
     * - 4 plan item definitions (one for each plan item)
     * - 4 plan items: 2 tasks and 2 milestones
     * - 4 sentries
     * - 3 entry criteria (on all plan items except taskA)
     */
    @Test
    public void testSimpleCmmnModelConversion() {
        CmmnModel cmmnModel = cmmnXmlConverter.convertToCmmnModel(getInputStreamProvider("simple-case.cmmn"));
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
        assertEquals("casePlanForm", planModel.getFormKey());

        // Sentries
        assertEquals(4, planModel.getSentries().size());
        for (Sentry sentry : planModel.getSentries()) {
            List<SentryOnPart> onParts = sentry.getOnParts();
            if (onParts != null && !onParts.isEmpty()) {
                assertEquals(1, onParts.size());
                assertNotNull(onParts.get(0).getId());
                assertNotNull(onParts.get(0).getSourceRef());
                assertNotNull(onParts.get(0).getSource());
                assertNotNull(onParts.get(0).getStandardEvent());
            } else {
                assertThat(sentry.getSentryIfPart().getCondition(), is("${true}"));
                assertThat(sentry.getName(), is("criterion name"));
            }
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

    /**
     * Same case model as in {@link #testSimpleCmmnModelConversion()}, but now with an exit criteria on the plan model.
     */
    @Test
    public void testExitCriteriaOnPlanModel() {
        CmmnModel cmmnModel = cmmnXmlConverter.convertToCmmnModel(getInputStreamProvider("exit-criteria-on-planmodel.cmmn"));
        Stage planModel = cmmnModel.getPrimaryCase().getPlanModel();
        assertEquals(4, planModel.getSentries().size());

        List<Criterion> exitCriteria = planModel.getExitCriteria();
        assertEquals(1, exitCriteria.size());
        Criterion criterion = exitCriteria.get(0);
        assertNotNull(criterion.getSentry());
        assertEquals("planItemMileStoneOne", criterion.getSentry().getOnParts().get(0).getSource().getId());
    }

    @Test
    public void testNestedStages() {
        CmmnModel cmmnModel = cmmnXmlConverter.convertToCmmnModel(getInputStreamProvider("nested-stages.cmmn"));
        Stage planModel = cmmnModel.getPrimaryCase().getPlanModel();
        assertEquals(2, planModel.getPlanItems().size());

        Stage nestedStage = null;
        for (PlanItem planItem : planModel.getPlanItems()) {
            assertNotNull(planItem.getPlanItemDefinition());
            if (planItem.getPlanItemDefinition() instanceof Stage) {
                nestedStage = (Stage) planItem.getPlanItemDefinition();
            }
        }
        assertNotNull(nestedStage);
        assertEquals("Nested Stage", nestedStage.getName());

        // Nested stage has 3 plan items, and one of them refereces the rootTook from the plan model
        assertEquals(3, nestedStage.getPlanItems().size());
        Stage nestedNestedStage = null;
        for (PlanItem planItem : nestedStage.getPlanItems()) {
            assertNotNull(planItem.getPlanItemDefinition());
            if (planItem.getPlanItemDefinition()  instanceof Stage) {
                nestedNestedStage = (Stage) planItem.getPlanItemDefinition();
            }
        }
        assertNotNull(nestedNestedStage);
        assertEquals("Nested Stage 2", nestedNestedStage.getName());
        assertEquals(1, nestedNestedStage.getPlanItems().size());
        assertEquals("rootTask", nestedNestedStage.getPlanItems().get(0).getPlanItemDefinition().getId());
    }

    @Test
    public void testMissingIdsAdded() {
        CmmnModel cmmnModel = cmmnXmlConverter.convertToCmmnModel(getInputStreamProvider("exit-criteria-on-planmodel.cmmn"));
        Stage planModel = cmmnModel.getPrimaryCase().getPlanModel();
        assertNotNull(planModel.getId());

        for (Sentry sentry : planModel.getSentries()) {
            assertNotNull(sentry.getId());
            for (SentryOnPart onPart : sentry.getOnParts()) {
                assertNotNull(onPart.getId());
            }
        }
    }

    private InputStreamProvider getInputStreamProvider(final String resourceName) {
        return new InputStreamProvider() {

            @Override
            public InputStream getInputStream() {
                return this.getClass().getClassLoader().getResourceAsStream(
                        this.getClass().getPackage().getName().replaceAll("\\.", "/") + "/" + resourceName);
            }

        };
    }

}
