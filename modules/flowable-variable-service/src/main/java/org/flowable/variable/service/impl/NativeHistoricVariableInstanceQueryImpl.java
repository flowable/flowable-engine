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
package org.flowable.variable.service.impl;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.query.AbstractNativeQuery;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.history.NativeHistoricVariableInstanceQuery;
import org.flowable.variable.service.VariableServiceConfiguration;

public class NativeHistoricVariableInstanceQueryImpl extends AbstractNativeQuery<NativeHistoricVariableInstanceQuery, HistoricVariableInstance> implements NativeHistoricVariableInstanceQuery {

    private static final long serialVersionUID = 1L;
    
    protected VariableServiceConfiguration variableServiceConfiguration;

    public NativeHistoricVariableInstanceQueryImpl(CommandContext commandContext, VariableServiceConfiguration variableServiceConfiguration) {
        super(commandContext);
        this.variableServiceConfiguration = variableServiceConfiguration;
    }

    public NativeHistoricVariableInstanceQueryImpl(CommandExecutor commandExecutor, VariableServiceConfiguration variableServiceConfiguration) {
        super(commandExecutor);
        this.variableServiceConfiguration = variableServiceConfiguration;
    }

    // results ////////////////////////////////////////////////////////////////

    @Override
    public List<HistoricVariableInstance> executeList(CommandContext commandContext, Map<String, Object> parameterMap) {
        return variableServiceConfiguration.getHistoricVariableInstanceEntityManager().findHistoricVariableInstancesByNativeQuery(parameterMap);
    }

    @Override
    public long executeCount(CommandContext commandContext, Map<String, Object> parameterMap) {
        return variableServiceConfiguration.getHistoricVariableInstanceEntityManager().findHistoricVariableInstanceCountByNativeQuery(parameterMap);
    }

}
