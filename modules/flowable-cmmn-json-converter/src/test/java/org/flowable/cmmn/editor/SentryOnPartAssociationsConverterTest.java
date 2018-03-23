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

import org.flowable.cmmn.model.Association;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.GraphicInfo;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryOnPart;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        assertNotNull(associations);
        assertEquals(5, associations.size());

        Map<String, List<Association>> byRelationShip = associations.stream()
                .collect(Collectors.groupingBy(a -> buildAssociationString(model, a)));

        assertEquals(5, byRelationShip.size());
        assertTrue(byRelationShip.containsKey("taskA|taskC|complete"));
        assertTrue(byRelationShip.containsKey("taskB|taskC|complete"));
        assertTrue(byRelationShip.containsKey("stage1|stage2|terminate"));
        //EXIT CRITERIA ARE "READ" BACKWARDS
        assertTrue(byRelationShip.containsKey("timedTask|expireTimer|occur"));
        assertTrue(byRelationShip.containsKey("stage2|abortStageTask|complete"));

        //Coordinates of associations are recalculated based on other elements?
        //This is only a stub that checks the "approximate" coordinates of 1 association
        Map<String, List<GraphicInfo>> associationsGraphicInfo = associations.stream()
                .collect(Collectors.toMap(Association::getId, a -> model.getFlowLocationGraphicInfo(a.getId())));
        assertEquals(5, associationsGraphicInfo.size());
        //Coordinates have a loss of precision during conversion and re-conversion
        double delta = 5.0;
        List<GraphicInfo> gInfo = associationsGraphicInfo.get("sid-F8E5EBC5-DCBB-4C01-B7EA-3267631B2625");
        assertEquals(286.0, gInfo.get(0).getX(), delta);
        assertEquals(175.0, gInfo.get(0).getY(), delta);
        assertEquals(345.0, gInfo.get(1).getX(), delta);
        assertEquals(199.0, gInfo.get(1).getY(), delta);


        //Check the Sentries OnParts
        //TASK C Entry Sentry
        PlanItemDefinition taskC = model.findPlanItemDefinition("taskC");
        assertNotNull(taskC);
        PlanItem taskBIntance = model.findPlanItem(taskC.getPlanItemRef());
        assertNotNull(taskBIntance);
        List<Criterion> taskCEntryCriterions = taskBIntance.getEntryCriteria();
        assertNotNull(taskCEntryCriterions);
        assertEquals(1, taskCEntryCriterions.size());
        Criterion criterion = taskCEntryCriterions.get(0);
        assertEquals("taskCEntrySentry", criterion.getId());
        Sentry sentry = criterion.getSentry();
        assertNotNull(sentry);
        List<SentryOnPart> onParts = sentry.getOnParts();
        assertNotNull(onParts);
        assertEquals(2, onParts.size());
        SentryOnPart sentryOnPart = onParts.get(0);
        assertEquals("taskA", sentryOnPart.getSource().getDefinitionRef());
        assertEquals(PlanItemTransition.COMPLETE, sentryOnPart.getStandardEvent());
        sentryOnPart = onParts.get(1);
        assertEquals("taskB", sentryOnPart.getSource().getDefinitionRef());
        assertEquals(PlanItemTransition.COMPLETE, sentryOnPart.getStandardEvent());

        //Stage 2
        PlanItemDefinition stage2 = model.findPlanItemDefinition("stage2");
        assertNotNull(stage2);
        PlanItem stage2Instance = model.findPlanItem(stage2.getPlanItemRef());
        assertNotNull(stage2Instance);
        //Stage 2 Entry Sentry
        List<Criterion> stage2EntryCriterions = stage2Instance.getEntryCriteria();
        assertNotNull(stage2EntryCriterions);
        assertEquals(1, stage2EntryCriterions.size());
        criterion = stage2EntryCriterions.get(0);
        assertEquals("stage1EntrySentry", criterion.getId());
        sentry = criterion.getSentry();
        assertNotNull(sentry);
        onParts = sentry.getOnParts();
        assertNotNull(onParts);
        assertEquals(1, onParts.size());
        sentryOnPart = onParts.get(0);
        assertEquals("stage1", sentryOnPart.getSource().getDefinitionRef());
        assertEquals(PlanItemTransition.TERMINATE, sentryOnPart.getStandardEvent());

        //Stage 2 Exit Sentry
        List<Criterion> stage2ExitCriterions = stage2Instance.getExitCriteria();
        assertNotNull(stage2ExitCriterions);
        assertEquals(1, stage2ExitCriterions.size());
        criterion = stage2ExitCriterions.get(0);
        assertEquals("stage2ExitSentry", criterion.getId());
        sentry = criterion.getSentry();
        assertNotNull(sentry);
        onParts = sentry.getOnParts();
        assertNotNull(onParts);
        assertEquals(1, onParts.size());
        sentryOnPart = onParts.get(0);
        assertEquals("abortStageTask", sentryOnPart.getSource().getDefinitionRef());
        assertEquals(PlanItemTransition.COMPLETE, sentryOnPart.getStandardEvent());

        //Timed Task Exit Sentry
        PlanItemDefinition timedTask = model.findPlanItemDefinition("timedTask");
        assertNotNull(timedTask);
        PlanItem timedtaskInstance = model.findPlanItem(timedTask.getPlanItemRef());
        assertNotNull(timedtaskInstance);
        List<Criterion> timedTaskExitCriterions = timedtaskInstance.getExitCriteria();
        assertNotNull(timedTaskExitCriterions);
        assertEquals(1, timedTaskExitCriterions.size());
        criterion = timedTaskExitCriterions.get(0);
        assertEquals("timedTaskExitSentry", criterion.getId());
        sentry = criterion.getSentry();
        assertNotNull(sentry);
        onParts = sentry.getOnParts();
        assertNotNull(onParts);
        assertEquals(1, onParts.size());
        sentryOnPart = onParts.get(0);
        assertEquals("expireTimer", sentryOnPart.getSource().getDefinitionRef());
        assertEquals(PlanItemTransition.OCCUR, sentryOnPart.getStandardEvent());

    }
}
