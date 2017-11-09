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

import org.flowable.engine.common.api.FlowableException;
import org.flowable.idm.api.GroupQuery;
import org.flowable.idm.api.UserQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SealIdentityServiceImpl extends IdmIdentityServiceImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(SealIdentityServiceImpl.class);

    @Override
    public UserQuery createUserQuery() {
        return new SealUserQueryImpl(this);
    }

    @Override
    public GroupQuery createGroupQuery() {
        return new SealGroupQueryImpl(this);
    }

    @Override
    public boolean checkPassword(String userId, String password) {
        if (password == null || password.length() == 0) {
            throw new FlowableException("Null or empty passwords are not allowed!");
        }
        return true;
    }

}
