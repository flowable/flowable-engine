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

package org.flowable.rest.service.api.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

/**
 * @author Frederik Heremans
 */
public class ProcessInstanceDiagramResourceTest extends BaseSpringRestTestCase {

    @Test
    @Deployment
    public void testGetProcessDiagram() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleProcess");

        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_DIAGRAM, processInstance.getId())),
                HttpStatus.SC_OK);
        assertNotNull(response.getEntity().getContent());
        assertEquals("image/png", response.getEntity().getContentType().getValue());
        closeResponse(response);
    }

    @Test
    @Deployment
    public void testGetProcessDiagramWithoutDiagram() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_DIAGRAM, processInstance.getId())), HttpStatus.SC_BAD_REQUEST));
    }

    /**
     * Test getting an unexisting process instance.
     */
    @Test
    public void testGetUnexistingProcessInstance() {
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_DIAGRAM, "unexistingpi")), HttpStatus.SC_NOT_FOUND));
    }
}
