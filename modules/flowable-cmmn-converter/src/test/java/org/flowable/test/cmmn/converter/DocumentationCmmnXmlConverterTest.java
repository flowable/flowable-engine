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
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Joram Barrez
 */
public class DocumentationCmmnXmlConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/documentation.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        String planModelDocumentation = cmmnModel.getPrimaryCase().getPlanModel().getDocumentation();
        assertThat(planModelDocumentation).isEqualTo("This is plan model documentation");

        String planItemDocumentation = cmmnModel.getPrimaryCase().getPlanModel().getPlanItems().get(0).getDocumentation();
        assertThat(planItemDocumentation).isEqualTo("This is plan item documentation");
    }

}
