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
package org.flowable.test.cmmn.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.IOParameter;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author martin.grofcik
 */
public class CaseTaskCmmnXmlConverterTest {


    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/case-task.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();

        // Case
        assertThat(cmmnModel.getCases())
                .extracting(Case::getId)
                .containsExactly("myCase");

        // Plan model
        Stage planModel = cmmnModel.getCases().get(0).getPlanModel();
        assertThat(planModel)
                .extracting(Stage::getId, Stage::getName)
                .containsExactly("myPlanModel", "My CasePlanModel");

        PlanItem planItemTask1 = cmmnModel.findPlanItem("planItem1");
        PlanItemDefinition planItemDefinition = planItemTask1.getPlanItemDefinition();
        assertThat(planItemDefinition)
                .isInstanceOfSatisfying(CaseTask.class, task1 -> {
                    assertThat(task1.getCaseRefExpression()).isEqualTo("caseDefinitionKey");
                    assertThat(task1.getFallbackToDefaultTenant()).isTrue();
                    assertThat(task1.isSameDeployment()).isTrue();

                    assertThat(task1.getInParameters())
                            .extracting(IOParameter::getSource, IOParameter::getTarget)
                            .containsExactly(tuple("testSource", "testTarget"));
                });
    }

}
