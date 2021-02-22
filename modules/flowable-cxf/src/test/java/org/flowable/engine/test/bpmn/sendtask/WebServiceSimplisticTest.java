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
package org.flowable.engine.test.bpmn.sendtask;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.bpmn.servicetask.AbstractWebServiceTaskTest;
import org.junit.jupiter.api.Test;

/**
 * @author Esteban Robles Luna
 * @author Falko Menge
 */
public class WebServiceSimplisticTest extends AbstractWebServiceTaskTest {

    @Override
    protected boolean isValidating() {
        return false;
    }

    @Test
    @Deployment
    public void testAsyncInvocationWithSimplisticDataFlow() throws Exception {
        assertThat(webServiceMock.getCount()).isEqualTo(-1);

        Map<String, Object> variables = new HashMap<>();
        variables.put("NewCounterValueVariable", 23);

        processEngine.getRuntimeService().startProcessInstanceByKey("asyncWebServiceInvocationWithSimplisticDataFlow", variables);
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        assertThat(webServiceMock.getCount()).isEqualTo(23);
    }
}
