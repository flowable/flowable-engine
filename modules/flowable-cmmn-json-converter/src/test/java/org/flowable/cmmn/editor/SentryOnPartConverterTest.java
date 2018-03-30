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
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryOnPart;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SentryOnPartConverterTest extends AbstractConverterTest {

    @Override
    protected String getResource() {
        return "test.sentryOnPartModel.json";
    }

    @Override
    protected void validateModel(CmmnModel model) {
        //Confirm a EntryCriteria attached to taskB sourced by "complete" transition of taskA
        PlanItemDefinition taskB = model.findPlanItemDefinition("taskB");
        assertNotNull(taskB);
        PlanItem taskBIntance = model.findPlanItem(taskB.getPlanItemRef());
        assertNotNull(taskBIntance);
        List<Criterion> entryCriterions = taskBIntance.getEntryCriteria();
        assertNotNull(entryCriterions);
        assertEquals(1, entryCriterions.size());
        Criterion criterion = entryCriterions.get(0);
        assertEquals("entryCriterion", criterion.getId());
        Sentry sentry = criterion.getSentry();
        assertNotNull(sentry);
        List<SentryOnPart> onParts = sentry.getOnParts();
        assertNotNull(onParts);
        assertEquals(1, onParts.size());
        SentryOnPart sentryOnPart = onParts.get(0);
        assertEquals("taskA", sentryOnPart.getSource().getDefinitionRef());
        assertEquals(PlanItemTransition.COMPLETE, sentryOnPart.getStandardEvent());
    }
}
