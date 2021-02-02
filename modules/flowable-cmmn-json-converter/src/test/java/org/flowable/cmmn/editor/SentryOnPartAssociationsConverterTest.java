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
import static org.assertj.core.data.Offset.offset;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.flowable.cmmn.model.Association;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.GraphicInfo;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryOnPart;

public class SentryOnPartAssociationsConverterTest extends AbstractConverterTest {

    private static String findAssociationEndPlanItemDefinitionId(CmmnModel model, String ref) {
        return Optional.ofNullable(model.findPlanItem(ref))
                .map(p -> p.getPlanItemDefinition().getId())
                .orElseGet(() -> Optional.ofNullable(model.getCriterion(ref))
                        .map(c -> model.findPlanItem(c.getAttachedToRefId()).getPlanItemDefinition().getId())
                        .orElse("invalid"));
    }

    private static String buildAssociationString(CmmnModel model, Association association) {

        String source = findAssociationEndPlanItemDefinitionId(model, association.getSourceRef());
        String target = findAssociationEndPlanItemDefinitionId(model, association.getTargetRef());

        return source + "|" + target + "|" + association.getTransitionEvent();
    }

    @Override
    protected String getResource() {
        return "test.SentryOnPartAssociationsModel.json";
    }

    @Override
    protected void validateModel(CmmnModel model) {
        List<Association> associations = model.getAssociations();
        assertThat(associations).hasSize(5);

        Map<String, List<Association>> byRelationShip = associations.stream()
                .collect(Collectors.groupingBy(a -> buildAssociationString(model, a)));

        assertThat(byRelationShip)
                .containsOnlyKeys("taskA|taskC|complete", "taskB|taskC|complete", "stage1|stage2|terminate",
                        //EXIT CRITERIA ARE "READ" BACKWARDS
                        "timedTask|expireTimer|occur", "stage2|abortStageTask|complete");

        //Coordinates of associations are recalculated based on other elements?
        //This is only a stub that checks the "approximate" coordinates of 1 association
        Map<String, List<GraphicInfo>> associationsGraphicInfo = associations.stream()
                .collect(Collectors.toMap(Association::getId, a -> model.getFlowLocationGraphicInfo(a.getId())));
        assertThat(associationsGraphicInfo).hasSize(5);
        //Coordinates have a loss of precision during conversion and re-conversion
        double delta = 5.0;
        List<GraphicInfo> gInfo = associationsGraphicInfo.get("sid-F8E5EBC5-DCBB-4C01-B7EA-3267631B2625");
        assertThat(gInfo.get(0).getX()).isCloseTo(286.0, offset(delta));
        assertThat(gInfo.get(0).getY()).isCloseTo(175.0, offset(delta));
        assertThat(gInfo.get(1).getX()).isCloseTo(345.0, offset(delta));
        assertThat(gInfo.get(1).getY()).isCloseTo(199.0, offset(delta));

        //Check the Sentries OnParts
        //TASK C Entry Sentry
        PlanItemDefinition taskC = model.findPlanItemDefinition("taskC");
        assertThat(taskC).isNotNull();
        PlanItem taskBIntance = model.findPlanItem(taskC.getPlanItemRef());
        assertThat(taskBIntance).isNotNull();
        List<Criterion> taskCEntryCriterions = taskBIntance.getEntryCriteria();
         assertThat(taskCEntryCriterions).hasSize(1);
        Criterion criterion = taskCEntryCriterions.get(0);
        assertThat(criterion.getId()).isEqualTo("taskCEntrySentry");
        Sentry sentry = criterion.getSentry();
        assertThat(sentry).isNotNull();
        List<SentryOnPart> onParts = sentry.getOnParts();
        assertThat(onParts).hasSize(2);
        SentryOnPart sentryOnPart = onParts.get(0);
        assertThat(sentryOnPart.getSource().getDefinitionRef()).isEqualTo("taskA");
        assertThat(sentryOnPart.getStandardEvent()).isEqualTo(PlanItemTransition.COMPLETE);
        sentryOnPart = onParts.get(1);
        assertThat(sentryOnPart.getSource().getDefinitionRef()).isEqualTo("taskB");
        assertThat(sentryOnPart.getStandardEvent()).isEqualTo(PlanItemTransition.COMPLETE);

        //Stage 2
        PlanItemDefinition stage2 = model.findPlanItemDefinition("stage2");
        assertThat(stage2).isNotNull();
        PlanItem stage2Instance = model.findPlanItem(stage2.getPlanItemRef());
        assertThat(stage2Instance).isNotNull();
        //Stage 2 Entry Sentry
        List<Criterion> stage2EntryCriterions = stage2Instance.getEntryCriteria();
        assertThat(stage2EntryCriterions).hasSize(1);
        criterion = stage2EntryCriterions.get(0);
        assertThat(criterion.getId()).isEqualTo("stage1EntrySentry");
        sentry = criterion.getSentry();
        assertThat(sentry).isNotNull();
        onParts = sentry.getOnParts();
        assertThat(onParts)
                .extracting(SentryOnPart::getStandardEvent)
                .containsExactly(PlanItemTransition.TERMINATE);
        sentryOnPart = onParts.get(0);
        assertThat(sentryOnPart.getSource().getDefinitionRef()).isEqualTo("stage1");

        //Stage 2 Exit Sentry
        List<Criterion> stage2ExitCriterions = stage2Instance.getExitCriteria();
        assertThat(stage2ExitCriterions).hasSize(1);
        criterion = stage2ExitCriterions.get(0);
        assertThat(criterion.getId()).isEqualTo("stage2ExitSentry");
        sentry = criterion.getSentry();
        assertThat(sentry).isNotNull();
        onParts = sentry.getOnParts();
        assertThat(onParts)
                .extracting(SentryOnPart::getStandardEvent)
                .containsExactly(PlanItemTransition.COMPLETE);
        sentryOnPart = onParts.get(0);
        assertThat(sentryOnPart.getSource().getDefinitionRef()).isEqualTo("abortStageTask");

        //Timed Task Exit Sentry
        PlanItemDefinition timedTask = model.findPlanItemDefinition("timedTask");
        assertThat(timedTask).isNotNull();
        PlanItem timedtaskInstance = model.findPlanItem(timedTask.getPlanItemRef());
        assertThat(timedtaskInstance).isNotNull();
        List<Criterion> timedTaskExitCriterions = timedtaskInstance.getExitCriteria();
        assertThat(timedTaskExitCriterions).hasSize(1);
        criterion = timedTaskExitCriterions.get(0);
        assertThat(criterion.getId()).isEqualTo("timedTaskExitSentry");
        sentry = criterion.getSentry();
        assertThat(sentry).isNotNull();
        onParts = sentry.getOnParts();
        assertThat(onParts)
                .extracting(SentryOnPart::getStandardEvent)
                .containsExactly(PlanItemTransition.OCCUR);
        sentryOnPart = onParts.get(0);
        assertThat(sentryOnPart.getSource().getDefinitionRef()).isEqualTo("expireTimer");
    }
}
