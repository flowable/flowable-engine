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
package org.flowable.engine.impl.persistence.entity.data.impl;

import java.util.List;

import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.AttachmentEntity;
import org.flowable.engine.impl.persistence.entity.AttachmentEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.AbstractProcessDataManager;
import org.flowable.engine.impl.persistence.entity.data.AttachmentDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisAttachmentDataManager extends AbstractProcessDataManager<AttachmentEntity> implements AttachmentDataManager {

    public MybatisAttachmentDataManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    @Override
    public Class<? extends AttachmentEntity> getManagedEntityClass() {
        return AttachmentEntityImpl.class;
    }

    @Override
    public AttachmentEntity create() {
        return new AttachmentEntityImpl();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AttachmentEntity> findAttachmentsByProcessInstanceId(String processInstanceId) {
        return getDbSqlSession().selectList("selectAttachmentsByProcessInstanceId", processInstanceId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AttachmentEntity> findAttachmentsByTaskId(String taskId) {
        return getDbSqlSession().selectList("selectAttachmentsByTaskId", taskId);
    }

}
