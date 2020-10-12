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
package org.flowable.common.engine.impl.persistence.entity;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;

/**
 * <p>
 * Encapsulates the logic for transparently working with {@link ByteArrayEntity} .
 * </p>
 *
 * @author Marcus Klimstra (CGI)
 */
public class ByteArrayRef implements Serializable {

    private static final long serialVersionUID = 1L;

    protected CommandExecutor commandExecutor;

    private String id;
    private String name;
    private ByteArrayEntity entity;
    protected boolean deleted;

    public ByteArrayRef() {
    }

    // Only intended to be used by ByteArrayRefTypeHandler
    public ByteArrayRef(String id, CommandExecutor commandExecutor) {
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

    /**
     * Returns the byte array from the {@link #getBytes(String)} method as {@link StandardCharsets#UTF_8} {@link String}.
     *
     * @return the byte array as {@link StandardCharsets#UTF_8} {@link String}
     */
    public String asString(String engineType) {
        byte[] bytes = getBytes(engineType);
        if (bytes == null) {
            return null;
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

    public void setValue(String name, byte[] bytes, String engineType) {
        this.name = name;
        setBytes(bytes, engineType);
    }

    /**
     * Set the specified {@link String} as the value of the byte array reference. It uses the
     * {@link StandardCharsets#UTF_8} charset to convert the {@link String} to the byte array.
     *
     * @param name the name of the byte array reference
     * @param value the value of the byte array reference
     */
    public void setValue(String name, String value, String engineType) {
        this.name = name;
        if (value != null) {
            setBytes(value.getBytes(StandardCharsets.UTF_8), engineType);
        }
    }

    protected void setBytes(byte[] bytes, String engineType) {
        if (id == null) {
            if (bytes != null) {
                AbstractEngineConfiguration engineConfiguration = getEngineConfiguration(engineType);
                ByteArrayEntityManager byteArrayEntityManager = engineConfiguration.getByteArrayEntityManager();
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

    public ByteArrayEntity getEntity(String engineType) {
        ensureInitialized(engineType);
        return entity;
    }

    public void delete(String engineType) {
        if (!deleted && id != null) {
            if (entity != null) {
                // if the entity has been loaded already,
                // we might as well use the safer optimistic locking delete.
                getEngineConfiguration(engineType).getByteArrayEntityManager().delete(entity);
            } else {
                getEngineConfiguration(engineType).getByteArrayEntityManager().deleteByteArrayById(id);
            }
            entity = null;
            id = null;
            deleted = true;
        }
    }

    protected void ensureInitialized(String engineType) {
        if (id != null && entity == null) {
            CommandContext commandContext = Context.getCommandContext();
            if (commandContext != null) {
                entity = getEngineConfiguration(engineType).getByteArrayEntityManager().findById(id);
            } else if (commandExecutor != null) {
                entity = commandExecutor.execute(context -> getEngineConfiguration(engineType).getByteArrayEntityManager().findById(id));
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
     * This makes a copy of this {@link ByteArrayRef}: a new
     * {@link ByteArrayRef} instance will be created, however with the same id,
     * name and {@link ByteArrayEntity} instances.
     */
    public ByteArrayRef copy() {
        ByteArrayRef copy = new ByteArrayRef();
        copy.id = id;
        copy.name = name;
        copy.entity = entity;
        copy.deleted = deleted;
        return copy;
    }
    
    protected AbstractEngineConfiguration getEngineConfiguration(String engineType) {
        CommandContext commandContext = Context.getCommandContext();
        if (commandContext != null) {
            return getEngineConfiguration(engineType, commandContext);

        } else if (commandExecutor != null) {
            return commandExecutor.execute(context -> {
                return getEngineConfiguration(engineType, context);
            });
            
        } else {
            throw new IllegalStateException("Cannot initialize byte array. There is no command context and there is no command Executor");
        }
    }

    protected AbstractEngineConfiguration getEngineConfiguration(String engineType, CommandContext commandContext) {
        // Although 'engineType' is passed here, due to backwards compatibility, it also can be a scopeType value.
        // For example, the scopeType of JobEntity determines which engine is used to retrieve the byteArrayEntityManager.
        // The 'all' on the next line is exactly that, see JobServiceConfiguration#JOB_EXECUTION_SCOPE_ALL
        if ("all".equalsIgnoreCase(engineType)) {
            return getEngineConfigurationForAllType(commandContext);
            
        } else {
            AbstractEngineConfiguration engineConfiguration = commandContext.getEngineConfigurations().get(engineType);
            if (engineConfiguration == null) {
                engineConfiguration = getFirstEngineConfigurationWithByteArrayEntityManager(commandContext);
            }
            
            return engineConfiguration;
        }
    }

    protected AbstractEngineConfiguration getEngineConfigurationForAllType(CommandContext commandContext) {
        AbstractEngineConfiguration engineConfiguration = commandContext.getEngineConfigurations().get(ScopeTypes.BPMN);
        if (engineConfiguration == null) {
            engineConfiguration = commandContext.getEngineConfigurations().get(ScopeTypes.CMMN);
            
            if (engineConfiguration == null) {
                engineConfiguration = getFirstEngineConfigurationWithByteArrayEntityManager(commandContext);
            }
        }
        
        if (engineConfiguration == null) {
            throw new IllegalStateException("Cannot initialize byte array. No engine configuration found");
        }
        
        return engineConfiguration;
    }
    
    protected AbstractEngineConfiguration getFirstEngineConfigurationWithByteArrayEntityManager(CommandContext commandContext) {
        AbstractEngineConfiguration engineConfiguration = null;
        for (AbstractEngineConfiguration possibleEngineConfiguration : commandContext.getEngineConfigurations().values()) {
            if (possibleEngineConfiguration.getByteArrayEntityManager() != null) {
                engineConfiguration = possibleEngineConfiguration;
                break;
            }
        }
        
        if (engineConfiguration == null) {
            throw new IllegalStateException("Cannot initialize byte array. No engine configuration found");
        }
        
        return engineConfiguration;
    }

    @Override
    public String toString() {
        return "ByteArrayRef[id=" + id + ", name=" + name + ", entity=" + entity + (deleted ? ", deleted]" : "]");
    }
}
