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

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.UserEventListener;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Dennis Federico
 */
public class UserEventListenerCmmnXmlConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/user-event-listener.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();

        List<HumanTask> humanTasks = cmmnModel.getPrimaryCase().getPlanModel().findPlanItemDefinitionsOfType(HumanTask.class, true);
        assertThat(humanTasks).hasSize(2);

        List<UserEventListener> userEventListeners = cmmnModel.getPrimaryCase().getPlanModel().findPlanItemDefinitionsOfType(UserEventListener.class, true);
        assertThat(userEventListeners)
                .extracting(UserEventListener::getName, UserEventListener::getId, UserEventListener::getDocumentation)
                .containsExactly(tuple("myUserEventListener", "userActionListener", "UserEventListener documentation"));
    }
}
