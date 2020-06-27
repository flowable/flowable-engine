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
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.DecisionTask;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;

/**
 * @author martin.grofcik
 */
public class DecisionTaskJsonConverterTest extends AbstractConverterTest {

    @Override
    protected String getResource() {
        return "test.dmnTaskModel.json";
    }

    @Override
    protected void validateModel(CmmnModel model) {
        Case caseModel = model.getPrimaryCase();
        assertThat(caseModel.getId()).isEqualTo("dmnExportCase");
        assertThat(caseModel.getName()).isEqualTo("dmnExportCase");

        Stage planModelStage = caseModel.getPlanModel();
        assertThat(planModelStage).isNotNull();
        assertThat(planModelStage.getId()).isEqualTo("casePlanModel");

        PlanItem planItem = planModelStage.findPlanItemInPlanFragmentOrUpwards("planItem1");
        assertThat(planItem).isNotNull();
        assertThat(planItem.getId()).isEqualTo("planItem1");
        assertThat(planItem.getName()).isEqualTo("dmnTask");
        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
        assertThat(planItemDefinition).isInstanceOf(DecisionTask.class);
        DecisionTask decisionTask = (DecisionTask) planItemDefinition;
        assertThat(decisionTask.getId()).isEqualTo("sid-F4BCA0C7-8737-4279-B50F-59272C7C65A2");
        assertThat(decisionTask.getName()).isEqualTo("dmnTask");

        assertThat(decisionTask.getFieldExtensions())
                .extracting(FieldExtension::getFieldName, FieldExtension::getStringValue)
                .as("fieldName, stringValue")
                .contains(
                        tuple("fallbackToDefaultTenant", "true"),
                        tuple("decisionTaskThrowErrorOnNoHits", "false")
                );
    }

}
