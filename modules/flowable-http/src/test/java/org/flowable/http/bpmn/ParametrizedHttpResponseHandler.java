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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.http.HttpResponse;
import org.flowable.http.delegate.HttpResponseHandler;

import static org.flowable.http.ExpressionUtils.getStringFromField;
import static org.flowable.http.ExpressionUtils.getStringSetFromField;

public class ParametrizedHttpResponseHandler implements HttpResponseHandler {

    protected Expression variableName;

    private static final long serialVersionUID = 1L;
    protected ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handleHttpResponse(VariableContainer execution, HttpResponse httpResponse) {
        try {
            JsonNode responseNode = objectMapper.readTree(httpResponse.getBody());
            String variableNameEvaluated = getStringFromField(this.variableName, execution);
            Set<String> fields = getStringSetFromField(variableNameEvaluated);
            for (final String field : fields) {
              execution.setVariable(field, responseNode.get("name").get(field).asText());
            }
            httpResponse.setBodyResponseHandled(true);

        } catch (Exception e) {
            // test handler
        }
    }

}
