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

import java.util.List;

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CasePageTask;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.ParentCompletionRule;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.junit.Test;

/**
 * @author Filip Hrisafov
 */
public class CasePageTaskSameDeploymentCmmnXmlConverterTest extends AbstractConverterTest {

    private static final String CMMN_RESOURCE = "org/flowable/test/cmmn/converter/casePageTaskSameDeployment.cmmn";

    @Test
    public void convertXMLToModel() throws Exception {
        CmmnModel cmmnModel = readXMLFile(CMMN_RESOURCE);
        validateModel(cmmnModel);
    }

    @Test
    public void convertModelToXML() throws Exception {
        CmmnModel cmmnModel = readXMLFile(CMMN_RESOURCE);
        CmmnModel parsedModel = exportAndReadXMLFile(cmmnModel);
        validateModel(parsedModel);
    }

    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();

        PlanItemDefinition casePageDefinition = cmmnModel.findPlanItemDefinition("casePageTaskSameDeployment");
        assertThat(casePageDefinition)
                .isInstanceOfSatisfying(CasePageTask.class, task -> {
                    assertThat(task.getFormKey()).isEqualTo("testKey");
                    assertThat(task.isSameDeployment()).isTrue();
                });
        assertThat(casePageDefinition.getAttributes()).isEmpty();


        casePageDefinition = cmmnModel.findPlanItemDefinition("casePageTaskSameDeploymentFalse");
        assertThat(casePageDefinition)
                .isInstanceOfSatisfying(CasePageTask.class, task -> {
                    assertThat(task.getFormKey()).isEqualTo("testKey2");
                    assertThat(task.isSameDeployment()).isFalse();
                });
        assertThat(casePageDefinition.getAttributes()).isEmpty();

        casePageDefinition = cmmnModel.findPlanItemDefinition("casePageTaskSameDeploymentGlobal");
        assertThat(casePageDefinition)
                .isInstanceOfSatisfying(CasePageTask.class, task -> {
                    assertThat(task.getFormKey()).isEqualTo("testKey3");
                    assertThat(task.isSameDeployment()).isTrue();
                });
        assertThat(casePageDefinition.getAttributes()).isEmpty();
    }

}
