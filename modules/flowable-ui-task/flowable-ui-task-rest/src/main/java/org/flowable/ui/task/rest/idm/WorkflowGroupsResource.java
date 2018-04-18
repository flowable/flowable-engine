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

import java.util.ArrayList;
import java.util.List;

import org.flowable.idm.api.Group;
import org.flowable.ui.common.model.GroupRepresentation;
import org.flowable.ui.common.model.ResultListDataRepresentation;
import org.flowable.ui.common.service.idm.RemoteIdmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app")
public class WorkflowGroupsResource {

    @Autowired
    private RemoteIdmService remoteIdmService;

    @RequestMapping(value = "/rest/workflow-groups", method = RequestMethod.GET)
    public ResultListDataRepresentation getGroups(@RequestParam(value = "filter", required = false) String filter) {

        List<? extends Group> matchingGroups = remoteIdmService.findGroupsByNameFilter(filter);
        List<GroupRepresentation> groupRepresentations = new ArrayList<>();
        for (Group group : matchingGroups) {
            groupRepresentations.add(new GroupRepresentation(group));
        }
        return new ResultListDataRepresentation(groupRepresentations);
    }

}
