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
package org.flowable.mongodb.persistence.manager;

import org.flowable.engine.impl.persistence.entity.ProcessDefinitionInfoEntity;
import org.flowable.engine.impl.persistence.entity.data.ProcessDefinitionInfoDataManager;

/**
 * @author Joram Barrez
 */
public class MongoDbProcessDefinitionInfoDataManager extends AbstractMongoDbDataManager implements ProcessDefinitionInfoDataManager {
    
    public static final String COLLECTION_PROCESS_DEFINITION_INFO = "processDefinitionInfo";

    @Override
    public ProcessDefinitionInfoEntity create() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProcessDefinitionInfoEntity findById(String entityId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insert(ProcessDefinitionInfoEntity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProcessDefinitionInfoEntity update(ProcessDefinitionInfoEntity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(ProcessDefinitionInfoEntity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProcessDefinitionInfoEntity findProcessDefinitionInfoByProcessDefinitionId(String processDefinitionId) {
        // TODO
        return null;
    }

}
