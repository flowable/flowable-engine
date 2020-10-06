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
package org.flowable.http.bpmn.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.flowable.http.common.impl.BaseHttpActivityDelegate.HTTP_TASK_REQUEST_FIELD_INVALID;
import static org.flowable.http.common.impl.BaseHttpActivityDelegate.HTTP_TASK_REQUEST_HEADERS_INVALID;
import static org.flowable.http.common.impl.BaseHttpActivityDelegate.HTTP_TASK_REQUEST_METHOD_INVALID;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.test.Deployment;
import org.flowable.http.bpmn.HttpServiceTaskTestCase;
import org.junit.jupiter.api.Test;

/**
 * @author Harsha Teja Kanna
 */
public class HttpServiceTaskValidationTest extends HttpServiceTaskTestCase {

    @Test
    @Deployment
    public void testInvalidProcess() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("validateProcess"))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage(HTTP_TASK_REQUEST_METHOD_INVALID);
    }

    @Test
    @Deployment
    public void testInvalidHeaders() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("invalidHeaders"))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage(HTTP_TASK_REQUEST_HEADERS_INVALID);
    }

    @Test
    @Deployment
    public void testInvalidFlags() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("invalidFlags"))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("String value \"Accept application/json\" is not allowed in boolean expression");
    }

    @Test
    @Deployment
    public void testInvalidTimeout() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("requestTimeout", "invalid");

        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("invalidTimeout", variables))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining(HTTP_TASK_REQUEST_FIELD_INVALID);
    }
}
