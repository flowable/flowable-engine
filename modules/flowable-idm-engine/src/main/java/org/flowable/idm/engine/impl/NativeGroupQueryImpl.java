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
package org.flowable.idm.engine.impl;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.AbstractNativeQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.NativeGroupQuery;
import org.flowable.idm.engine.impl.util.CommandContextUtil;

public class NativeGroupQueryImpl extends AbstractNativeQuery<NativeGroupQuery, Group> implements NativeGroupQuery {

    private static final long serialVersionUID = 1L;

    public NativeGroupQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public NativeGroupQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    // results ////////////////////////////////////////////////////////////////

    @Override
    public List<Group> executeList(CommandContext commandContext, Map<String, Object> parameterMap) {
        return CommandContextUtil.getGroupEntityManager(commandContext).findGroupsByNativeQuery(parameterMap);
    }

    @Override
    public long executeCount(CommandContext commandContext, Map<String, Object> parameterMap) {
        return CommandContextUtil.getGroupEntityManager(commandContext).findGroupCountByNativeQuery(parameterMap);
    }

}