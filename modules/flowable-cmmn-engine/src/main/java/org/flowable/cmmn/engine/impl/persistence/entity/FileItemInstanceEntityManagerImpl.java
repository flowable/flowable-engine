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

package org.flowable.cmmn.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.data.FileItemInstanceDataManager;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;

/**
 * @author Joram Barrez
 */
public class FileItemInstanceEntityManagerImpl extends AbstractCmmnEntityManager<FileItemInstanceEntity> implements FileItemInstanceEntityManager {

    protected FileItemInstanceDataManager fileItemInstanceDataManager;

    public FileItemInstanceEntityManagerImpl(CmmnEngineConfiguration cmmnEngineConfiguration, FileItemInstanceDataManager fileItemInstanceDataManager) {
        super(cmmnEngineConfiguration);
        this.fileItemInstanceDataManager = fileItemInstanceDataManager;
    }

    @Override
    protected DataManager<FileItemInstanceEntity> getDataManager() {
        return this.fileItemInstanceDataManager;
    }

    @Override
    public List<FileItemInstanceEntity> findCaseInstanceFileItemInstances(String caseInstanceId) {
        return fileItemInstanceDataManager.getCaseInstanceFileItemInstances(caseInstanceId);
    }

}
