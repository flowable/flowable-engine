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

package org.flowable.rest.service.api.engine.variable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.rest.variable.EngineRestVariable;

/**
 * Pojo representing a variable used in REST-service which defines it's name, variable, scope and type.
 * 
 * @author Frederik Heremans
 */
public class RestVariable extends EngineRestVariable {

    public enum RestVariableScope {
        LOCAL, GLOBAL
    }

    private RestVariableScope variableScope;

    @ApiModelProperty(example = "global", value = "Scope of the variable.", notes = "If local, the variable is explicitly defined on the resource it’s requested from. When global, the variable is defined on the parent (or any parent in the parent-tree) of the resource it’s requested from. When writing a variable and the scope is omitted, global is assumed.")
    @JsonIgnore
    public RestVariableScope getVariableScope() {
        return variableScope;
    }

    public void setVariableScope(RestVariableScope variableScope) {
        this.variableScope = variableScope;
    }

    public String getScope() {
        String scope = null;
        if (variableScope != null) {
            scope = variableScope.name().toLowerCase();
        }
        return scope;
    }

    public void setScope(String scope) {
        setVariableScope(getScopeFromString(scope));
    }

    public static RestVariableScope getScopeFromString(String scope) {
        if (scope != null) {
            for (RestVariableScope s : RestVariableScope.values()) {
                if (s.name().equalsIgnoreCase(scope)) {
                    return s;
                }
            }
            throw new FlowableIllegalArgumentException("Invalid variable scope: '" + scope + "'");
        } else {
            return null;
        }
    }
}
