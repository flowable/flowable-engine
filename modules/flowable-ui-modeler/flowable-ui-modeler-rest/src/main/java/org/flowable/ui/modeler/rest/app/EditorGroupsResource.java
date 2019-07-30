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
package org.flowable.ui.modeler.rest.app;

import org.flowable.ui.common.model.GroupRepresentation;
import org.flowable.ui.common.model.RemoteGroup;
import org.flowable.ui.common.model.ResultListDataRepresentation;
import org.flowable.ui.common.service.idm.RemoteIdmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Rest resource for managing groups, used in the editor app.
 */
@RestController
@RequestMapping("/app")
public class EditorGroupsResource {

    @Autowired
    protected RemoteIdmService remoteIdmService;

    @GetMapping(value = "/rest/editor-groups")
    public ResultListDataRepresentation getGroups(@RequestParam(required = false, value = "filter") String filter) {
        List<GroupRepresentation> result = new ArrayList<>();
        List<RemoteGroup> groups = remoteIdmService.findGroupsByNameFilter(filter);
        for (RemoteGroup group : groups) {
            result.add(new GroupRepresentation(group));
        }
        return new ResultListDataRepresentation(result);
    }
}
