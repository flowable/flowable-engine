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
package org.flowable.ui.idm.rest.api;

import org.flowable.idm.api.Group;
import org.flowable.ui.common.model.GroupRepresentation;
import org.flowable.ui.common.service.exception.NotFoundException;
import org.flowable.ui.idm.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
@RestController
public class ApiGroupsResource {

    @Autowired
    protected GroupService groupService;

    @GetMapping(value = "/idm/groups/{groupId}", produces = {"application/json"})
    public GroupRepresentation getGroupInformation(@PathVariable String groupId) {
        Group group = groupService.getGroup(groupId);
        if (group != null) {
            return new GroupRepresentation(group);

        } else {
            throw new NotFoundException();
        }
    }

    @GetMapping(value = "/idm/groups", produces = {"application/json"})
    public List<GroupRepresentation> findGroupsByFilter(@RequestParam("filter") String filter) {
        List<GroupRepresentation> result = new ArrayList<>();
        List<Group> groups = groupService.getGroups(filter);
        for (Group group : groups) {
            result.add(new GroupRepresentation(group));
        }
        return result;
    }

}
