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
package org.flowable.cmmn.engine.impl.idm;

import java.util.ArrayList;
import java.util.List;

import org.flowable.cmmn.api.CandidateManager;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.IdmIdentityService;

public class DefaultCandidateManager implements CandidateManager {
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;

    public DefaultCandidateManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    public List<String> getGroupsForCandidateUser(String candidateUser) {
        IdmIdentityService identityService = cmmnEngineConfiguration.getIdmIdentityService();
        List<Group> groups = identityService.createGroupQuery().groupMember(candidateUser).list();
        List<String> groupIds = new ArrayList<>();
        for (Group group : groups) {
            groupIds.add(group.getId());
        }
        return groupIds;
    }
}
