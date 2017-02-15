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

import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.idm.api.Picture;
import org.flowable.idm.api.User;
import org.flowable.idm.engine.impl.interceptor.Command;
import org.flowable.idm.engine.impl.interceptor.CommandContext;

/**
 * @author Tom Baeyens
 */
public class GetUserPictureCmd implements Command<Picture>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String userId;

    public GetUserPictureCmd(String userId) {
        this.userId = userId;
    }

    public Picture execute(CommandContext commandContext) {
        if (userId == null) {
            throw new FlowableIllegalArgumentException("userId is null");
        }

        User user = commandContext.getIdmEngineConfiguration().getIdmIdentityService()
                .createUserQuery().userId(userId)
                .singleResult();

        if (user == null) {
            throw new FlowableObjectNotFoundException("user " + userId + " doesn't exist", User.class);
        }

        return commandContext.getUserEntityManager().getUserPicture(user);
    }

}
