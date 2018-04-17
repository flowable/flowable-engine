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
package org.flowable.form.engine.impl;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.AbstractNativeQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.NativeFormDeploymentQuery;
import org.flowable.form.engine.impl.util.CommandContextUtil;

public class NativeFormDeploymentQueryImpl extends AbstractNativeQuery<NativeFormDeploymentQuery, FormDeployment> implements NativeFormDeploymentQuery {

    private static final long serialVersionUID = 1L;

    public NativeFormDeploymentQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public NativeFormDeploymentQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    // results ////////////////////////////////////////////////////////////////

    @Override
    public List<FormDeployment> executeList(CommandContext commandContext, Map<String, Object> parameterMap) {
        return CommandContextUtil.getDeploymentEntityManager(commandContext).findDeploymentsByNativeQuery(parameterMap);
    }

    @Override
    public long executeCount(CommandContext commandContext, Map<String, Object> parameterMap) {
        return CommandContextUtil.getDeploymentEntityManager(commandContext).findDeploymentCountByNativeQuery(parameterMap);
    }

}
