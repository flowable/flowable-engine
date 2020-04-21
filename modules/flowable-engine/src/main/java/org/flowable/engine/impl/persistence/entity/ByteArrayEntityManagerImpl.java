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

import java.util.List;

import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.data.ByteArrayDataManager;

/**
 * @author Joram Barrez
 * @author Marcus Klimstra (CGI)
 */
public class ByteArrayEntityManagerImpl
    extends AbstractProcessEngineEntityManager<ByteArrayEntity, ByteArrayDataManager>
    implements ByteArrayEntityManager {

    public ByteArrayEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, ByteArrayDataManager byteArrayDataManager) {
        super(processEngineConfiguration, byteArrayDataManager);
    }

    @Override
    public List<ByteArrayEntity> findAll() {
        return dataManager.findAll();
    }

    @Override
    public void deleteByteArrayById(String byteArrayEntityId) {
        dataManager.deleteByteArrayNoRevisionCheck(byteArrayEntityId);
    }

}
