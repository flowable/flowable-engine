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
package org.flowable.engine.data.inmemory.impl.common;

import java.util.List;
import java.util.stream.Collectors;

import org.flowable.common.engine.impl.persistence.entity.ByteArrayEntity;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayEntityImpl;
import org.flowable.common.engine.impl.persistence.entity.data.ByteArrayDataManager;
import org.flowable.engine.data.inmemory.AbstractMemoryDataManager;
import org.flowable.engine.data.inmemory.util.MapProvider;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In memory implementation of {@link ByteArrayDataManager}.
 *
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryByteArraydataManager extends AbstractMemoryDataManager<ByteArrayEntity> implements ByteArrayDataManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryByteArraydataManager.class);

    public MemoryByteArraydataManager(MapProvider mapProvider, ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(LOGGER, mapProvider, processEngineConfiguration.getIdGenerator());
    }

    @Override
    public ByteArrayEntity create() {
        return new ByteArrayEntityImpl();
    }

    @Override
    public ByteArrayEntity findById(String entityId) {
        return super.doFindById(entityId);
    }

    @Override
    public void insert(ByteArrayEntity entity) {
        super.doInsert(entity);
    }

    @Override
    public ByteArrayEntity update(ByteArrayEntity entity) {
        return super.doUpdate(entity);
    }

    @Override
    public void delete(String id) {
        super.doDelete(id);
    }

    @Override
    public void delete(ByteArrayEntity entity) {
        super.doDelete(entity);
    }

    @Override
    public List<ByteArrayEntity> findAll() {
        return getData().values().stream().collect(Collectors.toList());
    }

    @Override
    public void deleteByteArrayNoRevisionCheck(String byteArrayEntityId) {
        super.doDelete(byteArrayEntityId);
    }

    @Override
    public void bulkDeleteByteArraysNoRevisionCheck(List<String> byteArrayEntityIds) {
        if (byteArrayEntityIds ==null||byteArrayEntityIds.isEmpty()) {
            return;
        }
        byteArrayEntityIds.stream().forEach(e -> deleteByteArrayNoRevisionCheck(e));
    }
}
