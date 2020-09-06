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
package org.flowable.batch.service.impl.persistence.entity;

import java.io.Serializable;

import org.flowable.batch.service.BatchServiceConfiguration;
import org.flowable.batch.service.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;

/**
 * <p>
 * Encapsulates the logic for transparently working with {@link BatchByteArrayEntity} .
 * </p>
 * 
 * @author Marcus Klimstra (CGI)
 */
public class BatchByteArrayRef implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private BatchByteArrayEntity entity;
    protected boolean deleted;

    public BatchByteArrayRef() {
    }

    // Only intended to be used by ByteArrayRefTypeHandler
    public BatchByteArrayRef(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public byte[] getBytes(String engineType) {
        ensureInitialized(engineType);
        return (entity != null ? entity.getBytes() : null);
    }

    public void setValue(String name, byte[] bytes, String engineType) {
        this.name = name;
        setBytes(bytes, engineType);
    }

    private void setBytes(byte[] bytes, String engineType) {
        if (id == null) {
            if (bytes != null) {
                BatchByteArrayEntityManager byteArrayEntityManager = getBatchByteArrayEntityManager(
                        engineType, CommandContextUtil.getCommandContext());
                entity = byteArrayEntityManager.create();
                entity.setName(name);
                entity.setBytes(bytes);
                byteArrayEntityManager.insert(entity);
                id = entity.getId();
            }
        } else {
            ensureInitialized(engineType);
            entity.setBytes(bytes);
        }
    }

    public BatchByteArrayEntity getEntity(String engineType) {
        ensureInitialized(engineType);
        return entity;
    }

    public void delete(String engineType) {
        if (!deleted && id != null) {
            BatchByteArrayEntityManager byteArrayEntityManager = getBatchByteArrayEntityManager(
                    engineType, CommandContextUtil.getCommandContext());
            if (entity != null) {
                // if the entity has been loaded already,
                // we might as well use the safer optimistic locking delete.
                byteArrayEntityManager.delete(entity);
            } else {
                byteArrayEntityManager.deleteByteArrayById(id);
            }
            entity = null;
            id = null;
            deleted = true;
        }
    }

    private void ensureInitialized(String engineType) {
        if (id != null && entity == null) {
            BatchByteArrayEntityManager byteArrayEntityManager = getBatchByteArrayEntityManager(
                    engineType, CommandContextUtil.getCommandContext());
            entity = byteArrayEntityManager.findById(id);

            if (entity != null) {
                name = entity.getName();
            }
        }
    }

    public boolean isDeleted() {
        return deleted;
    }
    
    /**
     * This makes a copy of this {@link BatchByteArrayRef}: a new
     * {@link BatchByteArrayRef} instance will be created, however with the same id,
     * name and {@link BatchByteArrayEntity} instances.
     */
    public BatchByteArrayRef copy() {
        BatchByteArrayRef copy = new BatchByteArrayRef();
        copy.id = id;
        copy.name = name;
        copy.entity = entity;
        copy.deleted = deleted;
        return copy;
    }
    
    protected BatchByteArrayEntityManager getBatchByteArrayEntityManager(String engineType, CommandContext commandContext) {
        // Although 'engineType' is passed here, due to backwards compatibility, it also can be a scopeType value.
        // For example, the scopeType of JobEntity determines which engine is used to retrieve the byteArrayEntityManager.
        // The 'all' on the next line is exactly that, see JobServiceConfiguration#JOB_EXECUTION_SCOPE_ALL
        if ("all".equalsIgnoreCase(engineType)) {
            return getBatchByteArrayEntityManagerForAllType(commandContext);
            
        } else {
            AbstractEngineConfiguration engineConfiguration = commandContext.getEngineConfigurations().get(engineType);
            if (engineConfiguration == null) {
                return getBatchByteArrayEntityManager(commandContext);
            } else {
                return getBatchByteArrayEntityManager(engineConfiguration);
            }
        }
    }

    protected BatchByteArrayEntityManager getBatchByteArrayEntityManagerForAllType(CommandContext commandContext) {
        AbstractEngineConfiguration engineConfiguration = commandContext.getEngineConfigurations().get(ScopeTypes.BPMN);
        if (engineConfiguration == null) {
            engineConfiguration = commandContext.getEngineConfigurations().get(ScopeTypes.CMMN);
            
            if (engineConfiguration == null) {
                return getBatchByteArrayEntityManager(commandContext);
                
            } else {
                return getBatchByteArrayEntityManager(engineConfiguration);
            }
        }
        
        return getBatchByteArrayEntityManager(engineConfiguration);
    }
    
    protected BatchByteArrayEntityManager getBatchByteArrayEntityManager(CommandContext commandContext) {
        for (AbstractEngineConfiguration engineConfiguration : commandContext.getEngineConfigurations().values()) {
            if (engineConfiguration.getServiceConfigurations().containsKey(EngineConfigurationConstants.KEY_BATCH_SERVICE_CONFIG)) {
                return getBatchByteArrayEntityManager(engineConfiguration);
            }
        }
        
        throw new IllegalStateException("Cannot initialize byte array. No engine configuration found");
    }
    
    protected BatchByteArrayEntityManager getBatchByteArrayEntityManager(AbstractEngineConfiguration engineConfiguration) {
        BatchServiceConfiguration batchServiceConfiguration = (BatchServiceConfiguration)
                engineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_BATCH_SERVICE_CONFIG);
        return batchServiceConfiguration.getBatchByteArrayEntityManager();
    }

    @Override
    public String toString() {
        return "ByteArrayRef[id=" + id + ", name=" + name + ", entity=" + entity + (deleted ? ", deleted]" : "]");
    }
}
