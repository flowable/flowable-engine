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
package org.flowable.dmn.engine.impl;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.query.AbstractNativeQuery;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.api.NativeDecisionQuery;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;

public class NativeDecisionTableQueryImpl extends AbstractNativeQuery<NativeDecisionQuery, DmnDecision> implements NativeDecisionQuery {

    private static final long serialVersionUID = 1L;

    public NativeDecisionTableQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public NativeDecisionTableQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    // results ////////////////////////////////////////////////////////////////

    @Override
    public List<DmnDecision> executeList(CommandContext commandContext, Map<String, Object> parameterMap) {
        return CommandContextUtil.getDecisionEntityManager(commandContext).findDecisionsByNativeQuery(parameterMap);
    }

    @Override
    public long executeCount(CommandContext commandContext, Map<String, Object> parameterMap) {
        return CommandContextUtil.getDecisionEntityManager(commandContext).findDecisionCountByNativeQuery(parameterMap);
    }

}
