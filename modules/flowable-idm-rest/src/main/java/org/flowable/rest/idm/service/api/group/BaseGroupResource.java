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

package org.flowable.rest.idm.service.api.group;

import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.rest.idm.service.api.IdmRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class BaseGroupResource {

    @Autowired
    protected IdmRestResponseFactory restResponseFactory;

    @Autowired
    protected IdmIdentityService identityService;

    protected Group getGroupFromRequest(String groupId) {
        Group group = identityService.createGroupQuery().groupId(groupId).singleResult();

        if (group == null) {
            throw new FlowableObjectNotFoundException("Could not find a group with id '" + groupId + "'.", Group.class);
        }
        return group;
    }
    
}
