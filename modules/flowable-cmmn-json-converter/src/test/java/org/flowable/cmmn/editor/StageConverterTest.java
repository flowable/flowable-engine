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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.data.Offset.offset;

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
        assertThat(caseModel.getId()).isEqualTo("testCase");
        assertThat(caseModel.getName()).isEqualTo("Test case");

        Stage planModelStage = caseModel.getPlanModel();
        assertThat(planModelStage).isNotNull();
        assertThat(planModelStage.getId()).isEqualTo("myPlanModel");
        assertThat(planModelStage.getName()).isEqualTo("My plan model");
        assertThat(planModelStage.getDocumentation()).isEqualTo("My plan model documentation");
        assertThat(planModelStage.getFormKey()).isEqualTo("formKeyDefinition");
        assertThat(planModelStage.getValidateFormFields()).isEqualTo("validateFormFields");
        assertThat(planModelStage.isPlanModel()).isTrue();

        GraphicInfo graphicInfo = model.getGraphicInfo("myPlanModel");
        assertThat(graphicInfo.getX()).isCloseTo(30.0, offset(0.1));
        assertThat(graphicInfo.getY()).isCloseTo(45.0, offset(0.1));
        assertThat(graphicInfo.getWidth()).isCloseTo(819.0, offset(0.1));
        assertThat(graphicInfo.getHeight()).isCloseTo(713.0, offset(0.1));

        PlanItem planItem = planModelStage.findPlanItemInPlanFragmentOrUpwards("planItem1");
        assertThat(planItem).isNotNull();
        assertThat(planItem.getId()).isEqualTo("planItem1");
        assertThat(planItem.getName()).isEqualTo("Task");
        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
        assertThat(planItemDefinition).isInstanceOf(HumanTask.class);
        HumanTask humanTask = (HumanTask) planItemDefinition;
        assertThat(humanTask.getId()).isEqualTo("task1");
        assertThat(humanTask.getName()).isEqualTo("Task");

        assertThat(planItem.getEntryCriteria()).isEmpty();
        assertThat(planItem.getExitCriteria()).isEmpty();

        graphicInfo = model.getGraphicInfo("planItem1");
        assertThat(graphicInfo.getX()).isCloseTo(165.0, offset(0.1));
        assertThat(graphicInfo.getY()).isCloseTo(122.10, offset(0.1));
        assertThat(graphicInfo.getWidth()).isCloseTo(100.0, offset(0.1));
        assertThat(graphicInfo.getHeight()).isCloseTo(80.0, offset(0.1));

        PlanItem taskPlanItem = planModelStage.findPlanItemInPlanFragmentOrUpwards("planItem2");
        assertThat(planItem).isNotNull();
        assertThat(taskPlanItem.getId()).isEqualTo("planItem2");
        assertThat(taskPlanItem.getName()).isEqualTo("Task2");
        planItemDefinition = taskPlanItem.getPlanItemDefinition();
        assertThat(planItemDefinition).isInstanceOf(HumanTask.class);
        humanTask = (HumanTask) planItemDefinition;
        assertThat(humanTask.getId()).isEqualTo("task2");
        assertThat(humanTask.getName()).isEqualTo("Task2");

        assertThat(taskPlanItem.getEntryCriteria()).hasSize(1);
        assertThat(taskPlanItem.getExitCriteria()).isEmpty();

        graphicInfo = model.getGraphicInfo("planItem2");
        assertThat(graphicInfo.getX()).isCloseTo(405.0, offset(0.1));
        assertThat(graphicInfo.getY()).isCloseTo(120.0, offset(0.1));
        assertThat(graphicInfo.getWidth()).isCloseTo(100.0, offset(0.1));
        assertThat(graphicInfo.getHeight()).isCloseTo(80.0, offset(0.1));

        PlanItem milestonePlanItem = planModelStage.findPlanItemInPlanFragmentOrUpwards("planItem6");
        assertThat(milestonePlanItem).isNotNull();
        assertThat(milestonePlanItem.getId()).isEqualTo("planItem6");
        assertThat(milestonePlanItem.getName()).isEqualTo("Milestone 1");
        planItemDefinition = milestonePlanItem.getPlanItemDefinition();
        assertThat(planItemDefinition).isInstanceOf(Milestone.class);
        Milestone milestone = (Milestone) planItemDefinition;
        assertThat(milestone.getId()).isEqualTo("milestone1");
        assertThat(milestone.getName()).isEqualTo("Milestone 1");

        assertThat(milestonePlanItem.getEntryCriteria()).hasSize(1);
        assertThat(milestonePlanItem.getExitCriteria()).isEmpty();

        graphicInfo = model.getGraphicInfo("planItem6");
        assertThat(graphicInfo.getX()).isCloseTo(630.0, offset(0.1));
        assertThat(graphicInfo.getY()).isCloseTo(133.0, offset(0.1));
        assertThat(graphicInfo.getWidth()).isCloseTo(146.0, offset(0.1));
        assertThat(graphicInfo.getHeight()).isCloseTo(54.0, offset(0.1));

        List<Sentry> sentries = planModelStage.getSentries();
        assertThat(sentries).hasSize(2);

        Sentry sentry = sentries.get(0);

        Criterion criterion = taskPlanItem.getEntryCriteria().get(0);
        assertThat(criterion.getSentryRef()).isEqualTo(sentry.getId());

        assertThat(sentry.getOnParts())
                .extracting(SentryOnPart::getStandardEvent, SentryOnPart::getSourceRef)
                .containsExactly(tuple("complete", "planItem1"));

        sentry = sentries.get(1);

        criterion = milestonePlanItem.getEntryCriteria().get(0);
        assertThat(criterion.getSentryRef()).isEqualTo(sentry.getId());

        assertThat(sentry.getOnParts())
                .extracting(SentryOnPart::getStandardEvent, SentryOnPart::getSourceRef)
                .containsExactly(tuple("complete", "planItem2"));

        PlanItem stagePlanItem = planModelStage.findPlanItemInPlanFragmentOrUpwards("planItem5");
        assertThat(stagePlanItem).isNotNull();
        assertThat(stagePlanItem.getId()).isEqualTo("planItem5");
        assertThat(stagePlanItem.getName()).isEqualTo("Child stage");
        planItemDefinition = stagePlanItem.getPlanItemDefinition();
        assertThat(planItemDefinition).isInstanceOf(Stage.class);
        Stage stage = (Stage) planItemDefinition;
        assertThat(stage.getId()).isEqualTo("childStage");
        assertThat(stage.getName()).isEqualTo("Child stage");
        assertThat(stagePlanItem.getParentStage().getId()).isEqualTo(planModelStage.getId());

        assertThat(stagePlanItem.getEntryCriteria()).isEmpty();
        assertThat(stagePlanItem.getExitCriteria()).isEmpty();

        graphicInfo = model.getGraphicInfo("planItem5");
        assertThat(graphicInfo.getX()).isCloseTo(105.0, offset(0.1));
        assertThat(graphicInfo.getY()).isCloseTo(240.0, offset(0.1));
        assertThat(graphicInfo.getWidth()).isCloseTo(481.0, offset(0.1));
        assertThat(graphicInfo.getHeight()).isCloseTo(241.0, offset(0.1));

        assertThat(stage.getPlanItems()).hasSize(2);
        PlanItem subPlanItem1 = stage.findPlanItemInPlanFragmentOrUpwards("planItem3");
        assertThat(subPlanItem1).isNotNull();
        assertThat(subPlanItem1.getId()).isEqualTo("planItem3");
        assertThat(subPlanItem1.getName()).isEqualTo("Sub task 1");
        planItemDefinition = subPlanItem1.getPlanItemDefinition();
        assertThat(planItemDefinition).isInstanceOf(HumanTask.class);
        humanTask = (HumanTask) planItemDefinition;
        assertThat(humanTask.getId()).isEqualTo("subTask1");
        assertThat(humanTask.getName()).isEqualTo("Sub task 1");
        assertThat(subPlanItem1.getParentStage().getId()).isEqualTo(stage.getId());

        assertThat(subPlanItem1.getEntryCriteria()).hasSize(1);
        assertThat(subPlanItem1.getExitCriteria()).isEmpty();

        graphicInfo = model.getGraphicInfo("planItem3");
        assertThat(graphicInfo.getX()).isCloseTo(194.0, offset(0.1));
        assertThat(graphicInfo.getY()).isCloseTo(325.0, offset(0.1));
        assertThat(graphicInfo.getWidth()).isCloseTo(100.0, offset(0.1));
        assertThat(graphicInfo.getHeight()).isCloseTo(80.0, offset(0.1));

        PlanItem subPlanItem2 = stage.findPlanItemInPlanFragmentOrUpwards("planItem4");
        assertThat(subPlanItem2).isNotNull();
        assertThat(subPlanItem2.getId()).isEqualTo("planItem4");
        assertThat(subPlanItem2.getName()).isEqualTo("Sub task 2");
        planItemDefinition = subPlanItem2.getPlanItemDefinition();
        assertThat(planItemDefinition).isInstanceOf(HumanTask.class);
        humanTask = (HumanTask) planItemDefinition;
        assertThat(humanTask.getId()).isEqualTo("subTask2");
        assertThat(humanTask.getName()).isEqualTo("Sub task 2");
        assertThat(subPlanItem2.getParentStage().getId()).isEqualTo(stage.getId());

        assertThat(subPlanItem2.getEntryCriteria()).hasSize(1);
        assertThat(subPlanItem2.getExitCriteria()).isEmpty();

        graphicInfo = model.getGraphicInfo("planItem4");
        assertThat(graphicInfo.getX()).isCloseTo(390.0, offset(0.1));
        assertThat(graphicInfo.getY()).isCloseTo(325.0, offset(0.1));
        assertThat(graphicInfo.getWidth()).isCloseTo(100.0, offset(0.1));
        assertThat(graphicInfo.getHeight()).isCloseTo(80.0, offset(0.1));

        sentries = stage.getSentries();
        assertThat(sentries).hasSize(2);

        sentry = sentries.get(0);

        criterion = subPlanItem1.getEntryCriteria().get(0);
        assertThat(criterion.getSentryRef()).isEqualTo(sentry.getId());

        assertThat(sentry.getOnParts())
                .extracting(SentryOnPart::getStandardEvent, SentryOnPart::getSourceRef)
                .containsExactly(tuple("complete", "planItem2"));

        sentry = sentries.get(1);

        criterion = subPlanItem2.getEntryCriteria().get(0);
        assertThat(criterion.getSentryRef()).isEqualTo(sentry.getId());

        assertThat(sentry.getOnParts())
                .extracting(SentryOnPart::getStandardEvent, SentryOnPart::getSourceRef)
                .containsExactly(tuple("complete", "planItem3"));
    }
}
