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
package org.flowable.engine.impl;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.AbstractNativeQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.NativeExecutionQuery;

public class NativeExecutionQueryImpl extends AbstractNativeQuery<NativeExecutionQuery, Execution> implements NativeExecutionQuery {

    private static final long serialVersionUID = 1L;

    public NativeExecutionQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public NativeExecutionQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    // results ////////////////////////////////////////////////////////////////

    @Override
    public List<Execution> executeList(CommandContext commandContext, Map<String, Object> parameterMap) {
        return CommandContextUtil.getExecutionEntityManager(commandContext).findExecutionsByNativeQuery(parameterMap);
    }

    @Override
    public long executeCount(CommandContext commandContext, Map<String, Object> parameterMap) {
        return CommandContextUtil.getExecutionEntityManager(commandContext).findExecutionCountByNativeQuery(parameterMap);
    }

}
