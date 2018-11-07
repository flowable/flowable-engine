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

import java.util.List;

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.GraphicInfo;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.Milestone;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryOnPart;
import org.flowable.cmmn.model.Stage;

public class StageConverterTest extends AbstractConverterTest {

    @Override
    protected String getResource() {
        return "test.stagemodel.json";
    }

    @Override
    protected void validateModel(CmmnModel model) {
        Case caseModel = model.getPrimaryCase();
        assertEquals("testCase", caseModel.getId());
        assertEquals("Test case", caseModel.getName());

        Stage planModelStage = caseModel.getPlanModel();
        assertNotNull(planModelStage);
        assertEquals("myPlanModel", planModelStage.getId());
        assertEquals("My plan model", planModelStage.getName());
        assertEquals("My plan model documentation", planModelStage.getDocumentation());
        assertEquals("formKeyDefinition", planModelStage.getFormKey());
        assertTrue(planModelStage.isPlanModel());

        GraphicInfo graphicInfo = model.getGraphicInfo("myPlanModel");
        assertEquals(30.0, graphicInfo.getX(), 0.1);
        assertEquals(45.0, graphicInfo.getY(), 0.1);
        assertEquals(819.0, graphicInfo.getWidth(), 0.1);
        assertEquals(713.0, graphicInfo.getHeight(), 0.1);

        PlanItem planItem = planModelStage.findPlanItemInPlanFragmentOrUpwards("planItem1");
        assertNotNull(planItem);
        assertEquals("planItem1", planItem.getId());
        assertEquals("Task", planItem.getName());
        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
        assertNotNull(planItemDefinition);
        assertTrue(planItemDefinition instanceof HumanTask);
        HumanTask humanTask = (HumanTask) planItemDefinition;
        assertEquals("task1", humanTask.getId());
        assertEquals("Task", humanTask.getName());

        assertEquals(0, planItem.getEntryCriteria().size());
        assertEquals(0, planItem.getExitCriteria().size());

        graphicInfo = model.getGraphicInfo("planItem1");
        assertEquals(165.0, graphicInfo.getX(), 0.1);
        assertEquals(122.10, graphicInfo.getY(), 0.1);
        assertEquals(100.0, graphicInfo.getWidth(), 0.1);
        assertEquals(80.0, graphicInfo.getHeight(), 0.1);

        PlanItem taskPlanItem = planModelStage.findPlanItemInPlanFragmentOrUpwards("planItem2");
        assertNotNull(planItem);
        assertEquals("planItem2", taskPlanItem.getId());
        assertEquals("Task2", taskPlanItem.getName());
        planItemDefinition = taskPlanItem.getPlanItemDefinition();
        assertNotNull(planItemDefinition);
        assertTrue(planItemDefinition instanceof HumanTask);
        humanTask = (HumanTask) planItemDefinition;
        assertEquals("task2", humanTask.getId());
        assertEquals("Task2", humanTask.getName());

        assertEquals(1, taskPlanItem.getEntryCriteria().size());
        assertEquals(0, taskPlanItem.getExitCriteria().size());

        graphicInfo = model.getGraphicInfo("planItem2");
        assertEquals(405.0, graphicInfo.getX(), 0.1);
        assertEquals(120.0, graphicInfo.getY(), 0.1);
        assertEquals(100.0, graphicInfo.getWidth(), 0.1);
        assertEquals(80.0, graphicInfo.getHeight(), 0.1);

        PlanItem milestonePlanItem = planModelStage.findPlanItemInPlanFragmentOrUpwards("planItem6");
        assertNotNull(milestonePlanItem);
        assertEquals("planItem6", milestonePlanItem.getId());
        assertEquals("Milestone 1", milestonePlanItem.getName());
        planItemDefinition = milestonePlanItem.getPlanItemDefinition();
        assertNotNull(planItemDefinition);
        assertTrue(planItemDefinition instanceof Milestone);
        Milestone milestone = (Milestone) planItemDefinition;
        assertEquals("milestone1", milestone.getId());
        assertEquals("Milestone 1", milestone.getName());

        assertEquals(1, milestonePlanItem.getEntryCriteria().size());
        assertEquals(0, milestonePlanItem.getExitCriteria().size());

        graphicInfo = model.getGraphicInfo("planItem6");
        assertEquals(630.0, graphicInfo.getX(), 0.1);
        assertEquals(133.0, graphicInfo.getY(), 0.1);
        assertEquals(146.0, graphicInfo.getWidth(), 0.1);
        assertEquals(54.0, graphicInfo.getHeight(), 0.1);

        List<Sentry> sentries = planModelStage.getSentries();
        assertEquals(2, sentries.size());

        Sentry sentry = sentries.get(0);

        Criterion criterion = taskPlanItem.getEntryCriteria().get(0);
        assertEquals(sentry.getId(), criterion.getSentryRef());

        assertEquals(1, sentry.getOnParts().size());
        SentryOnPart onPart = sentry.getOnParts().get(0);
        assertEquals("complete", onPart.getStandardEvent());
        assertEquals("planItem1", onPart.getSourceRef());

        sentry = sentries.get(1);

        criterion = milestonePlanItem.getEntryCriteria().get(0);
        assertEquals(sentry.getId(), criterion.getSentryRef());

        assertEquals(1, sentry.getOnParts().size());
        onPart = sentry.getOnParts().get(0);
        assertEquals("complete", onPart.getStandardEvent());
        assertEquals("planItem2", onPart.getSourceRef());

        PlanItem stagePlanItem = planModelStage.findPlanItemInPlanFragmentOrUpwards("planItem5");
        assertNotNull(stagePlanItem);
        assertEquals("planItem5", stagePlanItem.getId());
        assertEquals("Child stage", stagePlanItem.getName());
        planItemDefinition = stagePlanItem.getPlanItemDefinition();
        assertNotNull(planItemDefinition);
        assertTrue(planItemDefinition instanceof Stage);
        Stage stage = (Stage) planItemDefinition;
        assertEquals("childStage", stage.getId());
        assertEquals("Child stage", stage.getName());
        assertEquals(planModelStage.getId(), stagePlanItem.getParentStage().getId());

        assertEquals(0, stagePlanItem.getEntryCriteria().size());
        assertEquals(0, stagePlanItem.getExitCriteria().size());

        graphicInfo = model.getGraphicInfo("planItem5");
        assertEquals(105.0, graphicInfo.getX(), 0.1);
        assertEquals(240.0, graphicInfo.getY(), 0.1);
        assertEquals(481.0, graphicInfo.getWidth(), 0.1);
        assertEquals(241.0, graphicInfo.getHeight(), 0.1);

        assertEquals(2, stage.getPlanItems().size());
        PlanItem subPlanItem1 = stage.findPlanItemInPlanFragmentOrUpwards("planItem3");
        assertNotNull(subPlanItem1);
        assertEquals("planItem3", subPlanItem1.getId());
        assertEquals("Sub task 1", subPlanItem1.getName());
        planItemDefinition = subPlanItem1.getPlanItemDefinition();
        assertNotNull(planItemDefinition);
        assertTrue(planItemDefinition instanceof HumanTask);
        humanTask = (HumanTask) planItemDefinition;
        assertEquals("subTask1", humanTask.getId());
        assertEquals("Sub task 1", humanTask.getName());
        assertEquals(stage.getId(), subPlanItem1.getParentStage().getId());

        assertEquals(1, subPlanItem1.getEntryCriteria().size());
        assertEquals(0, subPlanItem1.getExitCriteria().size());

        graphicInfo = model.getGraphicInfo("planItem3");
        assertEquals(194.0, graphicInfo.getX(), 0.1);
        assertEquals(325.0, graphicInfo.getY(), 0.1);
        assertEquals(100.0, graphicInfo.getWidth(), 0.1);
        assertEquals(80.0, graphicInfo.getHeight(), 0.1);

        PlanItem subPlanItem2 = stage.findPlanItemInPlanFragmentOrUpwards("planItem4");
        assertNotNull(subPlanItem2);
        assertEquals("planItem4", subPlanItem2.getId());
        assertEquals("Sub task 2", subPlanItem2.getName());
        planItemDefinition = subPlanItem2.getPlanItemDefinition();
        assertNotNull(planItemDefinition);
        assertTrue(planItemDefinition instanceof HumanTask);
        humanTask = (HumanTask) planItemDefinition;
        assertEquals("subTask2", humanTask.getId());
        assertEquals("Sub task 2", humanTask.getName());
        assertEquals(stage.getId(), subPlanItem2.getParentStage().getId());

        assertEquals(1, subPlanItem2.getEntryCriteria().size());
        assertEquals(0, subPlanItem2.getExitCriteria().size());

        graphicInfo = model.getGraphicInfo("planItem4");
        assertEquals(390.0, graphicInfo.getX(), 0.1);
        assertEquals(325.0, graphicInfo.getY(), 0.1);
        assertEquals(100.0, graphicInfo.getWidth(), 0.1);
        assertEquals(80.0, graphicInfo.getHeight(), 0.1);

        sentries = stage.getSentries();
        assertEquals(2, sentries.size());

        sentry = sentries.get(0);

        criterion = subPlanItem1.getEntryCriteria().get(0);
        assertEquals(sentry.getId(), criterion.getSentryRef());

        assertEquals(1, sentry.getOnParts().size());
        onPart = sentry.getOnParts().get(0);
        assertEquals("complete", onPart.getStandardEvent());
        assertEquals("planItem2", onPart.getSourceRef());

        sentry = sentries.get(1);

        criterion = subPlanItem2.getEntryCriteria().get(0);
        assertEquals(sentry.getId(), criterion.getSentryRef());

        assertEquals(1, sentry.getOnParts().size());
        onPart = sentry.getOnParts().get(0);
        assertEquals("complete", onPart.getStandardEvent());
        assertEquals("planItem3", onPart.getSourceRef());
    }
}
