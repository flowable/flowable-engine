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

package org.flowable.identitylink.service.impl.persistence.entity;

import java.util.List;

import org.flowable.engine.common.impl.persistence.entity.data.DataManager;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.identitylink.service.impl.persistence.entity.data.HistoricIdentityLinkDataManager;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class HistoricIdentityLinkEntityManagerImpl extends AbstractEntityManager<HistoricIdentityLinkEntity> implements HistoricIdentityLinkEntityManager {

    protected HistoricIdentityLinkDataManager historicIdentityLinkDataManager;

    public HistoricIdentityLinkEntityManagerImpl(IdentityLinkServiceConfiguration identityLinkServiceConfiguration, HistoricIdentityLinkDataManager historicIdentityLinkDataManager) {
        super(identityLinkServiceConfiguration);
        this.historicIdentityLinkDataManager = historicIdentityLinkDataManager;
    }

    @Override
    protected DataManager<HistoricIdentityLinkEntity> getDataManager() {
        return historicIdentityLinkDataManager;
    }

    @Override
    public List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByTaskId(String taskId) {
        return historicIdentityLinkDataManager.findHistoricIdentityLinksByTaskId(taskId);
    }

    @Override
    public List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByProcessInstanceId(String processInstanceId) {
        return historicIdentityLinkDataManager.findHistoricIdentityLinksByProcessInstanceId(processInstanceId);
    }

    @Override
    public void deleteHistoricIdentityLinksByTaskId(String taskId) {
        List<HistoricIdentityLinkEntity> identityLinks = findHistoricIdentityLinksByTaskId(taskId);
        for (HistoricIdentityLinkEntity identityLink : identityLinks) {
            delete(identityLink);
        }
    }

    @Override
    public void deleteHistoricIdentityLinksByProcInstance(final String processInstanceId) {

        List<HistoricIdentityLinkEntity> identityLinks = historicIdentityLinkDataManager
                .findHistoricIdentityLinksByProcessInstanceId(processInstanceId);

        for (HistoricIdentityLinkEntity identityLink : identityLinks) {
            delete(identityLink);
        }

    }

    public HistoricIdentityLinkDataManager getHistoricIdentityLinkDataManager() {
        return historicIdentityLinkDataManager;
    }

    public void setHistoricIdentityLinkDataManager(HistoricIdentityLinkDataManager historicIdentityLinkDataManager) {
        this.historicIdentityLinkDataManager = historicIdentityLinkDataManager;
    }

}
