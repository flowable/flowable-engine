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
package org.flowable.http;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.test.Deployment;

/**
 * @author Harsha Teja Kanna
 */
public class HttpServiceTaskTest extends HttpServiceTaskTestCase {

    @Deployment
    public void testSimpleGetOnly() throws Exception {
        String procId = runtimeService.startProcessInstanceByKey("simpleGetOnly").getId();
        assertProcessEnded(procId);
    }

    @Deployment
    public void testFailStatusCodes() throws Exception {
        try {
            runtimeService.startProcessInstanceByKey("failStatusCodes");
        } catch (Exception e) {
            assertEquals(true, e instanceof FlowableException);
            assertEquals("HTTP400", ((FlowableException) e).getMessage());
        }
    }

    @Deployment
    public void testHandleStatusCodes() throws Exception {
        String procId = runtimeService.startProcessInstanceByKey("handleStatusCodes").getId();
        assertProcessEnded(procId);
    }

    @Deployment
    public void testIgnoreException() throws Exception {
        String procId = runtimeService.startProcessInstanceByKey("ignoreException").getId();
        assertProcessEnded(procId);
    }

    @Deployment
    public void testMapException() throws Exception {
        String procId = runtimeService.startProcessInstanceByKey("mapException").getId();
        assertProcessEnded(procId);
    }
}
