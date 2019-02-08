/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.flowable.content.engine.impl.storage.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.content.api.ContentObject;
import org.flowable.content.api.ContentStorage;
import org.flowable.content.engine.ContentEngineConfiguration;
import org.flowable.content.engine.impl.storage.db.entity.StorageItemEntity;
import org.flowable.content.engine.impl.storage.db.entity.StorageItemEntityManager;
import org.flowable.content.engine.impl.storage.db.entity.StorageItemEntityManagerImpl;
import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;

/**
 * Implementation of {@link ContentStorage} that persists its data to a table in the database
 * 
 * @author Jorge Moraleda
 */
public class SimpleDatabaseContentStorage implements ContentStorage {

    private static TimeBasedGenerator UUID_GENERATOR = Generators.timeBasedGenerator(EthernetAddress.fromInterface());

    public static final String STORE_NAME = "database";
    private StorageItemDataManager storageItemDataManager;
    private StorageItemEntityManager storageItemEntityManager;
    
    public SimpleDatabaseContentStorage(ContentEngineConfiguration contentEngineConfiguration) {     
        this.storageItemDataManager = new MybatisStorageItemDataManager(contentEngineConfiguration);
        this.storageItemEntityManager = new StorageItemEntityManagerImpl(contentEngineConfiguration, storageItemDataManager);
    }

    @Override
    public ContentObject createContentObject(InputStream contentStream,
            Map<String, Object> metaData) {
        StorageItemEntity storageItemEntity = storageItemEntityManager.create();
        storageItemEntity.setId( UUID_GENERATOR.generate().toString() );
        addContentToEntity(storageItemEntity, contentStream);
        storageItemEntityManager.insert(storageItemEntity);
        return makeDatabaseContentObject(storageItemEntity);
    }

    @Override
    public ContentObject updateContentObject(String id, InputStream contentStream, Map<String, Object> metaDataString) {
        StorageItemEntity storageItemEntity = retrieveExistingEntity(id);
        addContentToEntity(storageItemEntity, contentStream);
        storageItemEntityManager.update(storageItemEntity);
        return makeDatabaseContentObject(storageItemEntity);
    }

    @Override
    public ContentObject getContentObject(String id) {
        StorageItemEntity storageItemEntity = retrieveExistingEntity(id);
        return makeDatabaseContentObject(storageItemEntity);
    }

    @Override
    public void deleteContentObject(String id) {
        storageItemEntityManager.delete(id);
    }
    
    protected void addContentToEntity(StorageItemEntity storageItemEntity, InputStream contentStream) {
        try {
            storageItemEntity.setBytes( IOUtils.toByteArray(contentStream) );
        } catch (IOException e) {
            throw new FlowableException(e.getMessage());
        }
    }
    
    protected DatabaseContentObject makeDatabaseContentObject(StorageItemEntity storageItemEntity) {
        return new DatabaseContentObject(storageItemEntity.getBytes(), storageItemEntity.getId());
    }
    
    protected StorageItemEntity retrieveExistingEntity(String id) {
        if (id == null) {
            throw new FlowableIllegalArgumentException("Storage id is null");
        }
        StorageItemEntity storageItemEntity = storageItemEntityManager.findById(id);
        if (storageItemEntity == null) {
            throw new FlowableObjectNotFoundException("no Storage Item found with id '" + id  + "'");
        }
        return storageItemEntity;
    }

    @Override
    public String getContentStoreName() {
        return STORE_NAME;
    }

    @Override
    public Map<String, Object> getMetaData() {
        // Currently not yet supported
        return null;
    }

}
