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

package org.flowable.rest.api.engine.variable;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.engine.variable.QueryVariable;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class QueryVariableTest extends BaseSpringRestTestCase {

    protected ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testSerializeQueryVariable() throws Exception {
        // Create a QueryVariable
        QueryVariable origQueryVariable = new QueryVariable();
        origQueryVariable.setName("name");
        origQueryVariable.setOperation("notEquals");
        origQueryVariable.setType("type");
        origQueryVariable.setValue("value");
        // Check that the "operation" is valid
        assertThat(origQueryVariable.getOperation()).isEqualTo("notEquals");
        assertThat(origQueryVariable.getVariableOperation()).isEqualTo(QueryVariable.QueryVariableOperation.NOT_EQUALS);

        // Serialize
        JsonNode jsonNode = objectMapper.convertValue(origQueryVariable, JsonNode.class);

        // Reconstitute the QueryVariable
        QueryVariable newQueryVariable = objectMapper.convertValue(jsonNode, QueryVariable.class);
        // Recheck the "operation" with the "new" variable
        assertThat(newQueryVariable.getVariableOperation()).isEqualTo(QueryVariable.QueryVariableOperation.NOT_EQUALS);
        assertThat(newQueryVariable.getOperation()).isEqualTo("notEquals");
    }
}
