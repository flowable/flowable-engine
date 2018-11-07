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
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryOnPart;
import org.flowable.cmmn.model.Stage;

public class SimpleConverterTest extends AbstractConverterTest {

    @Override
    protected String getResource() {
        return "test.simplemodel.json";
    }

    @Override
    protected void validateModel(CmmnModel model) {
        Case caseModel = model.getPrimaryCase();
        assertEquals("testModel", caseModel.getId());
        assertEquals("Test model", caseModel.getName());

        Stage planModelStage = caseModel.getPlanModel();
        assertNotNull(planModelStage);
        assertEquals("stage1", planModelStage.getId());

        GraphicInfo graphicInfo = model.getGraphicInfo("stage1");
        assertEquals(75.0, graphicInfo.getX(), 0.1);
        assertEquals(60.0, graphicInfo.getY(), 0.1);
        assertEquals(718.0, graphicInfo.getWidth(), 0.1);
        assertEquals(714.0, graphicInfo.getHeight(), 0.1);

        PlanItem planItem = planModelStage.findPlanItemInPlanFragmentOrUpwards("planItem1");
        assertNotNull(planItem);
        assertEquals("planItem1", planItem.getId());
        assertEquals("Task B", planItem.getName());
        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
        assertNotNull(planItemDefinition);
        assertTrue(planItemDefinition instanceof HumanTask);
        HumanTask humanTask = (HumanTask) planItemDefinition;
        assertEquals("task1", humanTask.getId());
        assertEquals("Task B", humanTask.getName());

        assertEquals(1, planItem.getEntryCriteria().size());
        assertEquals(0, planItem.getExitCriteria().size());

        graphicInfo = model.getGraphicInfo("planItem1");
        assertEquals(435.0, graphicInfo.getX(), 0.1);
        assertEquals(120.0, graphicInfo.getY(), 0.1);
        assertEquals(100.0, graphicInfo.getWidth(), 0.1);
        assertEquals(80.0, graphicInfo.getHeight(), 0.1);

        List<Sentry> sentries = planModelStage.getSentries();
        assertEquals(1, sentries.size());

        Sentry sentry = sentries.get(0);

        Criterion criterion = planItem.getEntryCriteria().get(0);
        assertEquals(sentry.getId(), criterion.getSentryRef());

        assertEquals(1, sentry.getOnParts().size());
        SentryOnPart onPart = sentry.getOnParts().get(0);
        assertEquals("complete", onPart.getStandardEvent());
        assertEquals("planItem2", onPart.getSourceRef());

    }
}
