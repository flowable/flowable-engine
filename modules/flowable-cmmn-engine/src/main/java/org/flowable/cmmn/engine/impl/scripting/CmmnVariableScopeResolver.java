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
package org.flowable.cmmn.engine.impl.scripting;

import java.util.Optional;
import java.util.stream.Stream;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.scripting.Resolver;
import org.flowable.variable.api.delegate.VariableScope;

/**
 *
 * @author Dennis Federico
 */
public class CmmnVariableScopeResolver implements Resolver {

    public enum CmmnEngineVariableScopeNames {
        EngineConfiguration("cmmnEngineConfiguration"),
        RuntimeService("cmmnRuntimeService"),
        HistoryService("cmmnHistoryService"),
        ManagementService("cmmnManagementService"),
        TaskService("cmmnTaskService"),
        Execution("planItemInstance");

        String name;

        CmmnEngineVariableScopeNames(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Stream<CmmnEngineVariableScopeNames> stream() {
            return Stream.of(CmmnEngineVariableScopeNames.values());
        }

        public static Optional<CmmnEngineVariableScopeNames> getVariableScopeByName(String name) {
            return CmmnEngineVariableScopeNames.stream().filter(v -> v.name.equals(name)).findFirst();
        }
    }

    protected CmmnEngineConfiguration engineConfiguration;
    protected VariableScope variableScope;

    public CmmnVariableScopeResolver(CmmnEngineConfiguration engineConfiguration, VariableScope variableScope) {
        if (variableScope == null) {
            throw new FlowableIllegalArgumentException("variableScope cannot be null");
        }
        this.variableScope = variableScope;
        this.engineConfiguration = engineConfiguration;
    }

    @Override
    public boolean containsKey(Object key) {
        return variableScope.hasVariable((String) key) || CmmnEngineVariableScopeNames.getVariableScopeByName((String) key).isPresent();
    }

    @Override
    public Object get(Object key) {
        return CmmnEngineVariableScopeNames.getVariableScopeByName((String) key)
                .map((t) -> {
                    switch (t) {
                        case EngineConfiguration:
                            return engineConfiguration;
                        case HistoryService:
                            return engineConfiguration.getCmmnHistoryService();
                        case ManagementService:
                            return engineConfiguration.getCmmnManagementService();
                        case RuntimeService:
                            return engineConfiguration.getCmmnRuntimeService();
                        case TaskService:
                            return engineConfiguration.getCmmnTaskService();
                        case Execution:
                            return variableScope;
                        default:
                            return null;
                    }
                })
                .orElse(variableScope.getVariable((String) key));
    }
}
