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

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.idm.engine.impl.util.CommandContextUtil;

/**
 * @author Tom Baeyens
 */
public class GetUserInfoKeysCmd implements Command<List<String>>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String userId;
    protected String userInfoType;

    public GetUserInfoKeysCmd(String userId, String userInfoType) {
        this.userId = userId;
        this.userInfoType = userInfoType;
    }

    @Override
    public List<String> execute(CommandContext commandContext) {
        return CommandContextUtil.getIdentityInfoEntityManager(commandContext).findUserInfoKeysByUserIdAndType(userId, userInfoType);
    }
}
