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
package org.flowable.cdi.test.api.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cdi.BusinessProcess;
import org.flowable.cdi.impl.annotation.StartProcessInterceptor;
import org.flowable.cdi.test.CdiFlowableTestCase;
import org.flowable.cdi.test.impl.beans.DeclarativeProcessController;
import org.flowable.engine.test.Deployment;
import org.junit.Test;

/**
 * Testcase for assuring that the {@link StartProcessInterceptor} behaves as expected.
 * 
 * @author Daniel Meyer
 */
public class StartProcessTest extends CdiFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/cdi/test/api/annotation/StartProcessTest.bpmn20.xml")
    public void testStartProcessByKey() {

        assertThat(runtimeService.createProcessInstanceQuery().singleResult()).isNull();

        getBeanInstance(DeclarativeProcessController.class).startProcessByKey();
        BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

        assertThat(runtimeService.createProcessInstanceQuery().singleResult()).isNotNull();

        assertThat((String)businessProcess.getVariable("name")).isEqualTo("Flowable");

        businessProcess.startTask(taskService.createTaskQuery().singleResult().getId());
        businessProcess.completeTask();
    }

    @Test
    @Deployment(resources = "org/flowable/cdi/test/api/annotation/StartProcessTest.bpmn20.xml")
    public void testStartProcessByName() {

        assertThat(runtimeService.createProcessInstanceQuery().singleResult()).isNull();

        getBeanInstance(DeclarativeProcessController.class).startProcessByName();

        BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

        assertThat(runtimeService.createProcessInstanceQuery().singleResult()).isNotNull();

        assertThat((String)businessProcess.getVariable("name")).isEqualTo("Flowable");

        businessProcess.startTask(taskService.createTaskQuery().singleResult().getId());
        businessProcess.completeTask();
    }

}
