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

package org.flowable.idm.engine.impl.cmd;

import java.io.Serializable;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.idm.api.PrivilegeMapping;
import org.flowable.idm.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 */
public class GetPrivilegeMappingsByPrivilegeIdCmd implements Command<List<PrivilegeMapping>>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String privilegeId;

    public GetPrivilegeMappingsByPrivilegeIdCmd(String privilegeId) {
        if (privilegeId == null) {
            throw new FlowableIllegalArgumentException("privilegeId is null");
        }
        this.privilegeId = privilegeId;
    }

    @Override
    public List<PrivilegeMapping> execute(CommandContext commandContext) {
        return CommandContextUtil.getPrivilegeMappingEntityManager(commandContext).getPrivilegeMappingsByPrivilegeId(privilegeId);
    }
}
