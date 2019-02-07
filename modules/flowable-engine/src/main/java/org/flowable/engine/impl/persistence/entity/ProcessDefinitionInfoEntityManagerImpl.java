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

package org.flowable.engine.impl.persistence.entity;

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.data.ProcessDefinitionInfoDataManager;

/**
 * @author Tijs Rademakers
 */
public class ProcessDefinitionInfoEntityManagerImpl extends
        AbstractEntityManager<ProcessDefinitionInfoEntity> implements ProcessDefinitionInfoEntityManager {

    protected ProcessDefinitionInfoDataManager processDefinitionInfoDataManager;

    public ProcessDefinitionInfoEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration,
            ProcessDefinitionInfoDataManager processDefinitionInfoDataManager) {

        super(processEngineConfiguration);
        this.processDefinitionInfoDataManager = processDefinitionInfoDataManager;
    }

    @Override
    protected DataManager<ProcessDefinitionInfoEntity> getDataManager() {
        return processDefinitionInfoDataManager;
    }

    @Override
    public void insertProcessDefinitionInfo(ProcessDefinitionInfoEntity processDefinitionInfo) {
        insert(processDefinitionInfo);
    }

    @Override
    public void updateProcessDefinitionInfo(ProcessDefinitionInfoEntity updatedProcessDefinitionInfo) {
        update(updatedProcessDefinitionInfo, true);
    }

    @Override
    public void deleteProcessDefinitionInfo(String processDefinitionId) {
        ProcessDefinitionInfoEntity processDefinitionInfo = findProcessDefinitionInfoByProcessDefinitionId(processDefinitionId);
        if (processDefinitionInfo != null) {
            delete(processDefinitionInfo);
            deleteInfoJson(processDefinitionInfo);
        }
    }

    @Override
    public void updateInfoJson(String id, byte[] json) {
        ProcessDefinitionInfoEntity processDefinitionInfo = findById(id);
        if (processDefinitionInfo != null) {
            ByteArrayRef ref = new ByteArrayRef(processDefinitionInfo.getInfoJsonId());
            ref.setValue("json", json);

            if (processDefinitionInfo.getInfoJsonId() == null) {
                processDefinitionInfo.setInfoJsonId(ref.getId());
            }
            
            updateProcessDefinitionInfo(processDefinitionInfo);
        }
    }

    @Override
    public void deleteInfoJson(ProcessDefinitionInfoEntity processDefinitionInfo) {
        if (processDefinitionInfo.getInfoJsonId() != null) {
            ByteArrayRef ref = new ByteArrayRef(processDefinitionInfo.getInfoJsonId());
            ref.delete();
        }
    }

    @Override
    public ProcessDefinitionInfoEntity findProcessDefinitionInfoByProcessDefinitionId(String processDefinitionId) {
        return processDefinitionInfoDataManager.findProcessDefinitionInfoByProcessDefinitionId(processDefinitionId);
    }

    @Override
    public byte[] findInfoJsonById(String infoJsonId) {
        ByteArrayRef ref = new ByteArrayRef(infoJsonId);
        return ref.getBytes();
    }
}