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

    public byte[] getBytes() {
        ensureInitialized();
        return (entity != null ? entity.getBytes() : null);
    }

    /**
     * Returns the byte array from the {@link #getBytes()} method as {@link StandardCharsets#UTF_8} {@link String}.
     *
     * @return the byte array as {@link StandardCharsets#UTF_8} {@link String}
     */
    public String asString() {
        byte[] bytes = getBytes();
        if (bytes == null) {
            return null;
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

    public void setValue(String name, byte[] bytes) {
        this.name = name;
        setBytes(bytes);
    }

    /**
     * Set the specified {@link String} as the value of the byte array reference. It uses the
     * {@link StandardCharsets#UTF_8} charset to convert the {@link String} to the byte array.
     *
     * @param name the name of the byte array reference
     * @param value the value of the byte array reference
     */
    public void setValue(String name, String value) {
        this.name = name;
        if (value != null) {
            setBytes(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void setBytes(byte[] bytes) {
        if (id == null) {
            if (bytes != null) {
                ByteArrayEntityManager byteArrayEntityManager = Context.getCommandContext().getCurrentEngineConfiguration().getByteArrayEntityManager();
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

    public ByteArrayEntity getEntity() {
        ensureInitialized();
        return entity;
    }

    public void delete() {
        if (!deleted && id != null) {
            if (entity != null) {
                // if the entity has been loaded already,
                // we might as well use the safer optimistic locking delete.
                Context.getCommandContext().getCurrentEngineConfiguration().getByteArrayEntityManager().delete(entity);
            } else {
                Context.getCommandContext().getCurrentEngineConfiguration().getByteArrayEntityManager().deleteByteArrayById(id);
            }
            entity = null;
            id = null;
            deleted = true;
        }
    }

    private void ensureInitialized() {
        if (id != null && entity == null) {
            CommandContext commandContext = Context.getCommandContext();
            if (commandContext != null) {
                entity = commandContext.getCurrentEngineConfiguration().getByteArrayEntityManager().findById(id);
            } else if (commandExecutor != null) {
                entity = commandExecutor.execute(context -> context.getCurrentEngineConfiguration().getByteArrayEntityManager().findById(id));
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

    @Override
    public String toString() {
        return "ByteArrayRef[id=" + id + ", name=" + name + ", entity=" + entity + (deleted ? ", deleted]" : "]");
    }
}
