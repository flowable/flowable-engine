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

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Filip Hrisafov
 */
public class HumanTaskSameDeploymentCmmnXmlConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/humanTaskSameDeployment.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();

        PlanItemDefinition itemDefinition = cmmnModel.findPlanItemDefinition("humanTaskSameDeployment");

        assertThat(itemDefinition)
                .isInstanceOfSatisfying(HumanTask.class, task -> {
                    assertThat(task.getFormKey()).isEqualTo("taskForm");
                    assertThat(task.isSameDeployment()).isTrue();
                    assertThat(task.getAttributes()).isEmpty();
                });

        itemDefinition = cmmnModel.findPlanItemDefinition("humanTaskSameDeploymentFalse");

        assertThat(itemDefinition)
                .isInstanceOfSatisfying(HumanTask.class, task -> {
                    assertThat(task.getFormKey()).isEqualTo("taskForm2");
                    assertThat(task.isSameDeployment()).isFalse();
                    assertThat(task.getAttributes()).isEmpty();
                });

        itemDefinition = cmmnModel.findPlanItemDefinition("humanTaskSameDeploymentGlobal");

        assertThat(itemDefinition)
                .isInstanceOfSatisfying(HumanTask.class, task -> {
                    assertThat(task.getFormKey()).isEqualTo("taskForm3");
                    assertThat(task.isSameDeployment()).isTrue();
                    assertThat(task.getAttributes()).isEmpty();
                });
    }

}
