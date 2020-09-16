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

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.IOParameter;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;

/**
 * @author martin.grofcik
 */
public class CaseTaskJsonConverterTest extends AbstractConverterTest {

    @Override
    protected String getResource() {
        return "test.caseTaskModel.json";
    }

    @Override
    protected void validateModel(CmmnModel model) {
        Case caseModel = model.getPrimaryCase();
        assertThat(caseModel.getId()).isEqualTo("caseTaskModelId");
        assertThat(caseModel.getName()).isEqualTo("caseTaskModelName");

        Stage planModelStage = caseModel.getPlanModel();
        assertThat(planModelStage).isNotNull();
        assertThat(planModelStage.getId()).isEqualTo("casePlanModel");

        PlanItem planItem = planModelStage.findPlanItemInPlanFragmentOrUpwards("planItem1");
        assertThat(planItem).isNotNull();
        assertThat(planItem.getId()).isEqualTo("planItem1");
        assertThat(planItem.getName()).isEqualTo("caseTaskName");
        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
        assertThat(planItemDefinition).isInstanceOf(CaseTask.class);
        CaseTask caseTask = (CaseTask) planItemDefinition;
        assertThat(caseTask.getId()).isEqualTo("sid-E06221FA-0225-4EF8-A1E8-8DC177326B77");
        assertThat(caseTask.getName()).isEqualTo("caseTaskName");
        assertThat(caseTask.getFallbackToDefaultTenant()).isTrue();
        assertThat(caseTask.isSameDeployment()).isTrue();

        assertThat(caseTask.getInParameters())
                .extracting(IOParameter::getSource, IOParameter::getTarget)
                .containsExactly(tuple("testSource", "testTarget"));
    }
}
