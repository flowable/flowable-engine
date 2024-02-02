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
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.IOParameter;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.ProcessTask;
import org.flowable.cmmn.model.Stage;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Tijs Rademakers
 */
public class ProcessTaskCmmnXmlConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/process-task.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();
        assertThat(cmmnModel.getCases()).hasSize(1);

        // Case
        Case caze = cmmnModel.getCases().get(0);
        assertThat(caze.getId()).isEqualTo("myCase");

        // Plan model
        Stage planModel = caze.getPlanModel();
        assertThat(planModel).isNotNull();
        assertThat(planModel.getId()).isEqualTo("myPlanModel");
        assertThat(planModel.getName()).isEqualTo("My CasePlanModel");

        PlanItem planItemTask1 = cmmnModel.findPlanItem("planItem1");
        PlanItemDefinition planItemDefinition = planItemTask1.getPlanItemDefinition();
        assertThat(planItemDefinition).isInstanceOf(ProcessTask.class);
        ProcessTask task1 = (ProcessTask) planItemDefinition;
        assertThat(task1.getProcessRefExpression()).isEqualTo("${processDefinitionKey}");
        assertThat((task1.isSameDeployment())).isNotNull();

        assertThat(task1.getInParameters())
                .extracting(IOParameter::getSource, IOParameter::getTarget)
                .containsExactly(tuple("num2", "num"));

        assertThat(task1.getOutParameters())
                .extracting(IOParameter::getSource, IOParameter::getTarget)
                .containsExactly(tuple("num", "num3"));

        assertThat(task1.getFallbackToDefaultTenant()).isTrue();
    }

}
