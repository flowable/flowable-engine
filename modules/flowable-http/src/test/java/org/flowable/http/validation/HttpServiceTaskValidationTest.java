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
package org.flowable.http.validation;

import static org.flowable.http.HttpActivityBehavior.HTTP_TASK_REQUEST_FIELD_INVALID;
import static org.flowable.http.HttpActivityBehavior.HTTP_TASK_REQUEST_HEADERS_INVALID;
import static org.flowable.http.HttpActivityBehavior.HTTP_TASK_REQUEST_METHOD_INVALID;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.test.Deployment;
import org.flowable.http.HttpServiceTaskTestCase;

/**
 * @author Harsha Teja Kanna
 */
public class HttpServiceTaskValidationTest extends HttpServiceTaskTestCase {
    @Deployment
    public void testInvalidProcess() {
        try {
            runtimeService.startProcessInstanceByKey("validateProcess");
        } catch (Exception e) {
            assertTrue(e instanceof FlowableException);
            assertEquals(HTTP_TASK_REQUEST_METHOD_INVALID, e.getMessage());
        }
    }

    @Deployment
    public void testInvalidHeaders() {
        try {
            runtimeService.startProcessInstanceByKey("invalidHeaders");
        } catch (Exception e) {
            assertTrue(e instanceof FlowableException);
            assertEquals(HTTP_TASK_REQUEST_HEADERS_INVALID, e.getMessage());
        }
    }

    @Deployment
    public void testInvalidFlags() {
        try {
            runtimeService.startProcessInstanceByKey("invalidFlags");
        } catch (Exception e) {
            assertTrue(e instanceof FlowableException);
            assertEquals(HTTP_TASK_REQUEST_FIELD_INVALID, e.getMessage());
        }
    }

    @Deployment
    public void testInvalidTimeout() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("requestTimeout", "invalid");

        try {
            runtimeService.startProcessInstanceByKey("invalidTimeout", variables);
        } catch (Exception e) {
            assertTrue(e instanceof FlowableException);
            assertTextPresent(HTTP_TASK_REQUEST_FIELD_INVALID, e.getMessage());
        }
    }
}
