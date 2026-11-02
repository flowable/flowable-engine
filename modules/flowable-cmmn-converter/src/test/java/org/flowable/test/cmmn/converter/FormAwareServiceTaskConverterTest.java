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
import org.flowable.cmmn.model.FormAwareServiceTask;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Christopher Welsch
 */
class FormAwareServiceTaskConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/formAwareServiceTask.cmmn")
    public void validateModelWithCommaSeparatedStringExpressionCandidates(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();

        PlanItemDefinition itemDefinition = cmmnModel.findPlanItemDefinition("task1");
        assertThat(itemDefinition)
                .isInstanceOfSatisfying(FormAwareServiceTask.class, formAwareServiceTask -> {
                    assertThat(formAwareServiceTask.getId()).isEqualTo("task1");
                    assertThat(formAwareServiceTask.getName()).isEqualTo("FormAwareServiceTask");
                    assertThat(formAwareServiceTask.getImplementationType())
                            .isEqualTo("delegateExpression");
                    assertThat(formAwareServiceTask.getValidateFormFields())
                            .isEqualTo("true");
                    assertThat(formAwareServiceTask.getFormKey())
                            .isEqualTo("someFormKey");
                });

    }

}
