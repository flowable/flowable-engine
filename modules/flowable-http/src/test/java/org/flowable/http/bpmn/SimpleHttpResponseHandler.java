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
package org.flowable.http.bpmn;

import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.http.common.api.HttpResponse;
import org.flowable.http.common.api.delegate.HttpResponseHandler;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class SimpleHttpResponseHandler implements HttpResponseHandler {

    protected ObjectMapper objectMapper = JsonMapper.shared();

    @Override
    public void handleHttpResponse(VariableContainer execution, HttpResponse httpResponse) {
        try {
            JsonNode responseNode = objectMapper.readTree(httpResponse.getBody());
            execution.setVariable("firstName", responseNode.get("name").get("firstName").asString());
            execution.setVariable("lastName", responseNode.get("name").get("lastName").asString());
            httpResponse.setBodyResponseHandled(true);

        } catch (Exception e) {
            // test handler
        }
    }

}
