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

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.variable.service.impl.util.CommandContextUtil;

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

    public byte[] getBytes() {
        ensureInitialized();
        return (entity != null ? entity.getBytes() : null);
    }

    public void setValue(String name, byte[] bytes) {
        this.name = name;
        setBytes(bytes);
    }

    private void setBytes(byte[] bytes) {
        if (id == null) {
            if (bytes != null) {
                VariableByteArrayEntityManager byteArrayEntityManager = CommandContextUtil.getByteArrayEntityManager();
                entity = byteArrayEntityManager.create();
                entity.setName(name);
                entity.setBytes(bytes);
                byteArrayEntityManager.insert(entity);
                id = entity.getId();
                deleted = false;
            }
        } else {
            ensureInitialized();
            if (bytes != null) {
                entity.setBytes(bytes);
            } else {
                // If the bytes are null delete this
                delete();
            }
        }
    }

    public VariableByteArrayEntity getEntity() {
        ensureInitialized();
        return entity;
    }

    public void delete() {
        if (!deleted && id != null) {
            if (entity != null) {
                // if the entity has been loaded already,
                // we might as well use the safer optimistic locking delete.
                CommandContextUtil.getByteArrayEntityManager().delete(entity);
            } else {
                CommandContextUtil.getByteArrayEntityManager().deleteByteArrayById(id);
            }
            entity = null;
            id = null;
            deleted = true;
        }
    }

    private void ensureInitialized() {
        if (id != null && entity == null) {
            CommandContext commandContext = CommandContextUtil.getCommandContext();
            if (commandContext != null) {
                entity = CommandContextUtil.getByteArrayEntityManager(commandContext).findById(id);
            } else if (commandExecutor != null) {
                entity = commandExecutor.execute(context -> CommandContextUtil.getByteArrayEntityManager(context).findById(id));
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

    @Override
    public String toString() {
        return "ByteArrayRef[id=" + id + ", name=" + name + ", entity=" + entity + (deleted ? ", deleted]" : "]");
    }
}
