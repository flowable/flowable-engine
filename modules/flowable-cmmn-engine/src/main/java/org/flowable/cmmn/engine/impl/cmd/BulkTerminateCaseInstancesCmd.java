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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Christopher Welsch
 */
public class BulkTerminateCaseInstancesCmd implements Command<Void> {

    protected Collection<String> caseInstanceIds;

    public BulkTerminateCaseInstancesCmd(Collection<String> caseInstanceIds) {
        this.caseInstanceIds = caseInstanceIds;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (caseInstanceIds == null) {
            throw new FlowableIllegalArgumentException("caseInstanceIds are null");
        }
        Set<String> instanceIdSet = new HashSet<>(caseInstanceIds);

        for (String instanceId : instanceIdSet) {
            CommandContextUtil.getAgenda(commandContext).planManualTerminateCaseInstanceOperation(instanceId);
        }
        return null;
    }
}
