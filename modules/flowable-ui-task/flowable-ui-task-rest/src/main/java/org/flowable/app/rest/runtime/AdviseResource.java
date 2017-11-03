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
package org.flowable.app.rest.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.flowable.app.model.runtime.AdviseRepresentation;
import org.flowable.app.model.runtime.TaskRepresentation;
import org.flowable.app.service.exception.InternalServerErrorException;
import org.flowable.app.service.runtime.FlowableTaskService;
import org.flowable.engine.RuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdviseResource {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AdviseResource.class);

    @Autowired
    protected FlowableTaskService taskService;
    
    @Autowired
    protected RuntimeService runtimeService;
    
    @Autowired
    protected Environment environment;

    @RequestMapping(value = "/rest/advise/{taskId}", method = RequestMethod.GET, produces = "application/json")
    public AdviseRepresentation adviseForTask(@PathVariable String taskId, HttpServletResponse response) {
        TaskRepresentation task = taskService.getTask(taskId, response);
        Map<String, Object> variableMap = runtimeService.getVariables(task.getProcessInstanceId());
        String adviseMessage = doAdviseCall((Long) variableMap.get("age"), (String) variableMap.get("nationality"));
        AdviseRepresentation adviseResponse = new AdviseRepresentation();
        adviseResponse.setAdvise(adviseMessage);
        return adviseResponse;
    } 

    protected String doAdviseCall(Long age, String nationality) {
        String adviseUrl = environment.getRequiredProperty("advise.rest.url");

        HttpGet httpGet = new HttpGet(adviseUrl + "?age=" + age + "&nationality=" + nationality);

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        CloseableHttpClient client = clientBuilder.build();

        try {
            HttpResponse response = client.execute(httpGet);
            InputStream responseContent = response.getEntity().getContent();
            String strResponse = IOUtils.toString(responseContent, "utf-8");
            LOGGER.info("Received advise response " + strResponse);
            return strResponse;
            
        } catch (Exception e) {
            LOGGER.error("Error calling advise service endpoint", e);
            throw new InternalServerErrorException("Error calling advise service endpoint: " + e.getMessage());
            
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    LOGGER.warn("Exception while closing http client", e);
                }
            }
        }
    }
}
