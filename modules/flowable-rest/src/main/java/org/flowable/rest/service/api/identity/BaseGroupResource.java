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

package org.flowable.rest.service.api.identity;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.IdentityService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.flowable.rest.service.api.RestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Frederik Heremans
 */
public class BaseGroupResource {

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected IdentityService identityService;
    
    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    protected Group getGroupFromRequest(String groupId) {
        Group group = identityService.createGroupQuery().groupId(groupId).singleResult();

        if (group == null) {
            throw new FlowableObjectNotFoundException("Could not find a group with id '" + groupId + "'.", User.class);
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessGroupInfoById(group);
        }
        
        return group;
    }
}
