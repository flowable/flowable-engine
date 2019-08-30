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
package org.flowable.cmmn.engine.impl.persistence.entity.data.impl;

import java.util.List;
import java.util.Objects;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.FileItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.FileItemInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.AbstractCmmnDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.FileItemInstanceDataManager;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcherAdapter;

/**
 * @author Joram Barrez
 */
public class MybatisFileItemInstanceDataManager extends AbstractCmmnDataManager<FileItemInstanceEntity> implements FileItemInstanceDataManager {

    protected CachedEntityMatcherAdapter<FileItemInstanceEntity> fileItemInstanceByCaseInstanceIdMatcher = new CachedEntityMatcherAdapter<FileItemInstanceEntity>() {
        @Override
        public boolean isRetained(FileItemInstanceEntity fileItemInstanceEntity, Object param) {
            return Objects.equals(fileItemInstanceEntity.getCaseInstanceId(), param);
        }
    };

    public MybatisFileItemInstanceDataManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }

    @Override
    public Class<? extends FileItemInstanceEntity> getManagedEntityClass() {
        return FileItemInstanceEntityImpl.class;
    }

    @Override
    public FileItemInstanceEntity create() {
        return new FileItemInstanceEntityImpl();
    }

    @Override
    public List<FileItemInstanceEntity> getCaseInstanceFileItemInstances(String caseInstanceId) {
        return getList("selectFileItemInstancesByCaseInstanceId", caseInstanceId, fileItemInstanceByCaseInstanceIdMatcher, true);
    }

}
