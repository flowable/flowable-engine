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
package org.flowable.engine.test.bpmn.servicetask;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * @author Esteban Robles Luna
 */
public class WebServiceSimplisticTest extends AbstractWebServiceTaskTest {

    @Override
    protected boolean isValidating() {
        return false;
    }

    @Test
    @Deployment
    public void testWebServiceInvocationWithSimplisticDataFlow() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("PrefixVariable", "The counter has the value ");
        variables.put("SuffixVariable", ". Good news");

        ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("webServiceInvocationWithSimplisticDataFlow", variables);
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        String response = (String) processEngine.getRuntimeService().getVariable(instance.getId(), "OutputVariable");
        assertThat(response).isEqualTo("The counter has the value -1. Good news");
    }

    @Test
    @Deployment
    public void testWebResponseNoName() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("PrefixVariable", "The counter has the value ");
        variables.put("SuffixVariable", ". Good news (NO NAME)");

        ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("webServiceInvocationWithSimplisticDataFlow", variables);
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        String response = (String) processEngine.getRuntimeService().getVariable(instance.getId(), "OutputVariable");
        assertThat(response).isEqualTo("The counter has the value -1. Good news (NO NAME)");
    }

    @Test
    @Deployment
    public void testWebResponseKeywordName() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("PrefixVariable", "The counter has the value ");
        variables.put("SuffixVariable", ". Good news Keyword");

        ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("webServiceInvocationWithSimplisticDataFlow", variables);
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        String response = (String) processEngine.getRuntimeService().getVariable(instance.getId(), "OutputVariable");
        assertThat(response).isEqualTo("The counter has the value -1. Good news Keyword");
    }
}
