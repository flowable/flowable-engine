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
import org.flowable.cmmn.model.HttpServiceTask;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Filip Hrisafov
 */
public class HttpServiceTaskParallelInSameTransactionConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/http-service-task-parallelInSameTransaction.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();

        Stage planModel = cmmnModel.getPrimaryCase()
                .getPlanModel();

        PlanItemDefinition itemDefinition = planModel.findPlanItemDefinitionInStageOrDownwards("httpTaskA");
        assertThat(itemDefinition).isInstanceOf(HttpServiceTask.class);
        HttpServiceTask httpServiceTask = (HttpServiceTask) itemDefinition;
        assertThat(httpServiceTask.getParallelInSameTransaction()).isTrue();

        itemDefinition = planModel.findPlanItemDefinitionInStageOrDownwards("httpTaskB");
        assertThat(itemDefinition).isInstanceOf(HttpServiceTask.class);
        httpServiceTask = (HttpServiceTask) itemDefinition;
        assertThat(httpServiceTask.getParallelInSameTransaction()).isFalse();

        itemDefinition = planModel.findPlanItemDefinitionInStageOrDownwards("httpTaskC");
        assertThat(itemDefinition).isInstanceOf(HttpServiceTask.class);
        httpServiceTask = (HttpServiceTask) itemDefinition;
        assertThat(httpServiceTask.getParallelInSameTransaction()).isNull();

    }

}
