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

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Stage;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Filip Hrisafov
 */
public class PlanModelSameDeploymentGlobalCmmnXmlConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/planModelSameDeploymentGlobal.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();

        Case primaryCase = cmmnModel.getPrimaryCase();
        assertThat(primaryCase).isNotNull();

        Stage planModel = primaryCase.getPlanModel();
        assertThat(planModel).isNotNull();
        assertThat(planModel.getFormKey()).isEqualTo("formKey");
        assertThat(planModel.isSameDeployment()).isTrue();
        assertThat(planModel.getAttributes()).isEmpty();
    }

}
