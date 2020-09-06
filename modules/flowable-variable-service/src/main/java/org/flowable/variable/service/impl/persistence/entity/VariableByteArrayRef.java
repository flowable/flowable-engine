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
package org.flowable.variable.service.impl.persistence.entity;

import java.io.Serializable;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.variable.service.VariableServiceConfiguration;

/**
 * <p>
 * Encapsulates the logic for transparently working with {@link VariableByteArrayEntity} .
 * </p>
 * 
 * @author Marcus Klimstra (CGI)
 */
public class VariableByteArrayRef implements Serializable {

    private static final long serialVersionUID = 1L;

    protected CommandExecutor commandExecutor;

    private String id;
    private String name;
    private VariableByteArrayEntity entity;
    protected boolean deleted;

    public VariableByteArrayRef() {
    }

    // Only intended to be used by ByteArrayRefTypeHandler
    public VariableByteArrayRef(String id, CommandExecutor commandExecutor) {
        this.id = id;
        this.commandExecutor = commandExecutor;
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
                VariableByteArrayEntityManager byteArrayEntityManager = getVariableByteArrayEntityManager(engineType);
                entity = byteArrayEntityManager.create();
                entity.setName(name);
                entity.setBytes(bytes);
                byteArrayEntityManager.insert(entity);
                id = entity.getId();
                deleted = false;
            }
        } else {
            ensureInitialized(engineType);
            if (bytes != null) {
                entity.setBytes(bytes);
            } else {
                // If the bytes are null delete this
                delete(engineType);
            }
        }
    }

    public VariableByteArrayEntity getEntity(String engineType) {
        ensureInitialized(engineType);
        return entity;
    }

    public void delete(String engineType) {
        if (!deleted && id != null) {
            VariableByteArrayEntityManager byteArrayEntityManager = getVariableByteArrayEntityManager(engineType);
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
            CommandContext commandContext = Context.getCommandContext();
            if (commandContext != null) {
                entity = getVariableByteArrayEntityManager(engineType).findById(id);
            } else if (commandExecutor != null) {
                entity = commandExecutor.execute(context -> getVariableByteArrayEntityManager(engineType).findById(id));
            } else {
                throw new IllegalStateException("Cannot initialize byte array. There is no command context and there is no command Executor");
            }

            if (entity != null) {
                name = entity.getName();
            }
        }
    }

    public boolean isDeleted() {
        return deleted;
    }
    
    /**
     * This makes a copy of this {@link VariableByteArrayRef}: a new
     * {@link VariableByteArrayRef} instance will be created, however with the same id,
     * name and {@link VariableByteArrayEntity} instances.
     */
    public VariableByteArrayRef copy() {
        VariableByteArrayRef copy = new VariableByteArrayRef();
        copy.id = id;
        copy.name = name;
        copy.entity = entity;
        copy.deleted = deleted;
        return copy;
    }
    
    protected VariableByteArrayEntityManager getVariableByteArrayEntityManager(String engineType) {
        CommandContext commandContext = Context.getCommandContext();
        if (commandContext != null) {
            return getVariableByteArrayEntityManager(engineType, commandContext);

        } else if (commandExecutor != null) {
            return commandExecutor.execute(context -> {
                return getVariableByteArrayEntityManager(engineType, context);
            });
            
        } else {
            throw new IllegalStateException("Cannot initialize byte array. There is no command context and there is no command Executor");
        }
    }
    
    protected VariableByteArrayEntityManager getVariableByteArrayEntityManager(String engineType, CommandContext commandContext) {
        // Although 'engineType' is passed here, due to backwards compatibility, it also can be a scopeType value.
        // For example, the scopeType of JobEntity determines which engine is used to retrieve the byteArrayEntityManager.
        // The 'all' on the next line is exactly that, see JobServiceConfiguration#JOB_EXECUTION_SCOPE_ALL
        if ("all".equalsIgnoreCase(engineType)) {
            return getVariableByteArrayEntityManagerForAllType(commandContext);
            
        } else {
            AbstractEngineConfiguration engineConfiguration = commandContext.getEngineConfigurations().get(engineType);
            if (engineConfiguration == null) {
                return getVariableByteArrayEntityManager(commandContext);
            } else {
                return getVariableByteArrayEntityManager(engineConfiguration);
            }
        }
    }

    protected VariableByteArrayEntityManager getVariableByteArrayEntityManagerForAllType(CommandContext commandContext) {
        AbstractEngineConfiguration engineConfiguration = commandContext.getEngineConfigurations().get(ScopeTypes.BPMN);
        if (engineConfiguration == null) {
            engineConfiguration = commandContext.getEngineConfigurations().get(ScopeTypes.CMMN);
            
            if (engineConfiguration == null) {
                return getVariableByteArrayEntityManager(commandContext);
                
            } else {
                return getVariableByteArrayEntityManager(engineConfiguration);
            }
        }
        
        return getVariableByteArrayEntityManager(engineConfiguration);
    }
    
    protected VariableByteArrayEntityManager getVariableByteArrayEntityManager(CommandContext commandContext) {
        for (AbstractEngineConfiguration engineConfiguration : commandContext.getEngineConfigurations().values()) {
            if (engineConfiguration.getServiceConfigurations().containsKey(EngineConfigurationConstants.KEY_VARIABLE_SERVICE_CONFIG)) {
                return getVariableByteArrayEntityManager(engineConfiguration);
            }
        }
        
        throw new IllegalStateException("Cannot initialize byte array. No engine configuration found");
    }
    
    protected VariableByteArrayEntityManager getVariableByteArrayEntityManager(AbstractEngineConfiguration engineConfiguration) {
        VariableServiceConfiguration variableServiceConfiguration = (VariableServiceConfiguration)
                engineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_VARIABLE_SERVICE_CONFIG);
        return variableServiceConfiguration.getByteArrayEntityManager();
    }

    @Override
    public String toString() {
        return "ByteArrayRef[id=" + id + ", name=" + name + ", entity=" + entity + (deleted ? ", deleted]" : "]");
    }
}
