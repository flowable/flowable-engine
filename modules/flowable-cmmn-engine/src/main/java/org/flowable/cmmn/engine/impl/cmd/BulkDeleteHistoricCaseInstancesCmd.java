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

package org.flowable.cmmn.engine.impl.cmd;

import java.io.Serializable;
import java.util.Collection;

import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

public class BulkDeleteHistoricCaseInstancesCmd implements Command<Object>, Serializable {

    private static final long serialVersionUID = 1L;

    protected Collection<String> caseInstanceIds;

    public BulkDeleteHistoricCaseInstancesCmd(Collection<String> caseInstanceIds) {
        this.caseInstanceIds = caseInstanceIds;
    }

    @Override
    public Object execute(CommandContext commandContext) {
        if (caseInstanceIds == null) {
            throw new FlowableIllegalArgumentException("historic case instanceIds are null");
        }

        if (caseInstanceIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("historic case instanceIds are empty");
        }
        
        CommandContextUtil.getCmmnHistoryManager(commandContext).recordBulkDeleteHistoricCaseInstances(caseInstanceIds);
        
        return null;
    }

}
