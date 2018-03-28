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

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryOnPart;
import org.flowable.cmmn.model.Stage;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Dennis Federico
 */
public class UserEventListenerConverterTest extends AbstractConverterTest {

    @Override
    protected String getResource() {
        return "test.userEventListenerModel.json";
    }

    @Override
    protected void validateModel(CmmnModel cmmnModel) {
        Stage model = cmmnModel.getPrimaryCase().getPlanModel();
        assertEquals(4, model.getPlanItemDefinitionMap().size());


        assertNotNull(model.getPlanItemDefinitionMap().get("taskA"));
        assertNotNull(model.getPlanItemDefinitionMap().get("taskB"));
        assertNotNull(model.getPlanItemDefinitionMap().get("startTaskAUserEvent"));
        assertNotNull(model.getPlanItemDefinitionMap().get("stopTaskBUserEvent"));

        Map<String, Sentry> sentries = model.getSentries().stream()
                .collect(Collectors.toMap(Sentry::getName, Function.identity(), (s1, s2) -> s1));
        assertEquals(2, sentries.size());
        Sentry sentry = sentries.get("entryTaskASentry");
        assertNotNull(sentry);
        List<SentryOnPart> onParts = sentry.getOnParts();
        assertEquals(1, onParts.size());
        SentryOnPart onPart = onParts.get(0);
        assertEquals(PlanItemTransition.OCCUR, onPart.getStandardEvent());
        assertEquals("startTaskAUserEvent", onPart.getSource().getPlanItemDefinition().getId());
        PlanItem task = model.findPlanItemInPlanFragmentOrUpwards(model.findPlanItemDefinition("taskA").getPlanItemRef());
        List<Criterion> criterions = task.getEntryCriteria();
        assertNotNull(criterions);
        assertEquals(1, criterions.size());
        assertEquals(criterions.get(0).getSentryRef(), sentry.getId());


        sentry = sentries.get("exitTaskBSentry");
        assertNotNull(sentry);
        onParts = sentry.getOnParts();
        assertEquals(1, onParts.size());
        onPart = onParts.get(0);
        assertEquals(PlanItemTransition.OCCUR, onPart.getStandardEvent());
        assertEquals("stopTaskBUserEvent", onPart.getSource().getPlanItemDefinition().getId());
        task = model.findPlanItemInPlanFragmentOrUpwards(model.findPlanItemDefinition("taskB").getPlanItemRef());
        criterions = task.getExitCriteria();
        assertNotNull(criterions);
        assertEquals(1, criterions.size());
        assertEquals(criterions.get(0).getSentryRef(), sentry.getId());

    }

}