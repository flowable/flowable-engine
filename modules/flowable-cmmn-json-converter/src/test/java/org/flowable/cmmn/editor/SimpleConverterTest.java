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
        assertThat(caseModel.getId()).isEqualTo("testModel");
        assertThat(caseModel.getName()).isEqualTo("Test model");

        Stage planModelStage = caseModel.getPlanModel();
        assertThat(planModelStage).isNotNull();
        assertThat(planModelStage.getId()).isEqualTo("stage1");

        GraphicInfo graphicInfo = model.getGraphicInfo("stage1");
        assertThat(graphicInfo.getX()).isCloseTo(75.0, offset(0.1));
        assertThat(graphicInfo.getY()).isCloseTo(60.0, offset(0.1));
        assertThat(graphicInfo.getWidth()).isCloseTo(718.0, offset(0.1));
        assertThat(graphicInfo.getHeight()).isCloseTo(714.0, offset(0.1));

        PlanItem planItem = planModelStage.findPlanItemInPlanFragmentOrUpwards("planItem1");
        assertThat(planItem).isNotNull();
        assertThat(planItem.getId()).isEqualTo("planItem1");
        assertThat(planItem.getName()).isEqualTo("Task B");
        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
        assertThat(planItemDefinition).isInstanceOf(HumanTask.class);
        HumanTask humanTask = (HumanTask) planItemDefinition;
        assertThat(humanTask.getId()).isEqualTo("task1");
        assertThat(humanTask.getName()).isEqualTo("Task B");

        assertThat(planItem.getEntryCriteria()).hasSize(1);
        assertThat(planItem.getExitCriteria()).isEmpty();

        graphicInfo = model.getGraphicInfo("planItem1");
        assertThat(graphicInfo.getX()).isCloseTo(435.0, offset(0.1));
        assertThat(graphicInfo.getY()).isCloseTo(120.0, offset(0.1));
        assertThat(graphicInfo.getWidth()).isCloseTo(100.0, offset(0.1));
        assertThat(graphicInfo.getHeight()).isCloseTo(80.0, offset(0.1));

        List<Sentry> sentries = planModelStage.getSentries();
        assertThat(sentries).hasSize(1);

        Sentry sentry = sentries.get(0);

        Criterion criterion = planItem.getEntryCriteria().get(0);
        assertThat(criterion.getSentryRef()).isEqualTo(sentry.getId());

        assertThat(sentry.getOnParts())
                .extracting(SentryOnPart::getStandardEvent, SentryOnPart::getSourceRef)
                .containsExactly(tuple("complete", "planItem2"));

    }
}
