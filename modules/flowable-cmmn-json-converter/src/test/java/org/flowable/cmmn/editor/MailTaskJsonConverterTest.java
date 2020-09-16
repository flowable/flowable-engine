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
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.cmmn.model.Stage;

/**
 * @author Filip Hrisafov
 */
public class MailTaskJsonConverterTest extends AbstractConverterTest {

    @Override
    protected String getResource() {
        return "test.mailTaskModel.json";
    }

    @Override
    protected void validateModel(CmmnModel model) {
        Case caseModel = model.getPrimaryCase();
        assertThat(caseModel.getId()).isEqualTo("mailTaskCase");

        Stage planModelStage = caseModel.getPlanModel();
        assertThat(planModelStage).isNotNull();

        PlanItemDefinition mailTaskDefinition = planModelStage.findPlanItemDefinitionInStageOrDownwards("mailTask");
        assertThat(mailTaskDefinition).isInstanceOf(ServiceTask.class);
        ServiceTask mailServiceTask = (ServiceTask) mailTaskDefinition;
        assertThat(mailServiceTask.getType()).isEqualTo(ServiceTask.MAIL_TASK);
        assertThat(mailServiceTask.getFieldExtensions())
                .extracting(FieldExtension::getFieldName, FieldExtension::getStringValue)
                .containsExactlyInAnyOrder(
                        tuple("headers", "X-Test: test"),
                        tuple("to", "test-to@test.com"),
                        tuple("from", "test-from@test.com"),
                        tuple("subject", "Test Subject"),
                        tuple("cc", "test-cc@test.com"),
                        tuple("bcc", "test-bcc@test.com"),
                        tuple("text", "Test Text"),
                        tuple("textVar", "testTextVar"),
                        tuple("html", "Test HTML"),
                        tuple("htmlVar", "testHtmlVar"),
                        tuple("charset", "UTF-8")
                );
    }
}
