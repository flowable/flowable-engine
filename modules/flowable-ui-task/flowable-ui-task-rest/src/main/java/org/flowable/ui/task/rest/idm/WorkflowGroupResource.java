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
package org.flowable.ui.task.rest.idm;

import javax.servlet.http.HttpServletResponse;

import org.flowable.idm.api.Group;
import org.flowable.ui.common.model.GroupRepresentation;
import org.flowable.ui.common.service.exception.NotFoundException;
import org.flowable.ui.common.service.idm.RemoteIdmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app")
public class WorkflowGroupResource {

    @Autowired
    private RemoteIdmService remoteIdmService;

    @RequestMapping(value = "/rest/workflow-groups/{groupId}", method = RequestMethod.GET)
    public GroupRepresentation getGroup(@PathVariable String groupId, HttpServletResponse response) {
        Group group = remoteIdmService.getGroup(groupId);

        if (group == null) {
            throw new NotFoundException("Group with id: " + groupId + " does not exist or is inactive");
        }

        return new GroupRepresentation(group);
    }

}
