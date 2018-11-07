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
package org.flowable.job.service.impl.persistence.entity;

import java.io.Serializable;

import org.flowable.job.service.impl.util.CommandContextUtil;
import java.nio.charset.StandardCharsets;

/**
 * <p>
 * Encapsulates the logic for transparently working with {@link JobByteArrayEntity} .
 * </p>
 *
 * @author Marcus Klimstra (CGI)
 */
public class JobByteArrayRef implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private JobByteArrayEntity entity;
    protected boolean deleted;

    public JobByteArrayRef() {
    }

    // Only intended to be used by ByteArrayRefTypeHandler
    public JobByteArrayRef(String id) {
        this.id = id;
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
                JobByteArrayEntityManager byteArrayEntityManager = CommandContextUtil.getJobByteArrayEntityManager();
                entity = byteArrayEntityManager.create();
                entity.setName(name);
                entity.setBytes(bytes);
                byteArrayEntityManager.insert(entity);
                id = entity.getId();
            }
        } else {
            ensureInitialized();
            entity.setBytes(bytes);
        }
    }

    public JobByteArrayEntity getEntity() {
        ensureInitialized();
        return entity;
    }

    public void delete() {
        if (!deleted && id != null) {
            if (entity != null) {
                // if the entity has been loaded already,
                // we might as well use the safer optimistic locking delete.
                CommandContextUtil.getJobByteArrayEntityManager().delete(entity);
            } else {
                CommandContextUtil.getJobByteArrayEntityManager().deleteByteArrayById(id);
            }
            entity = null;
            id = null;
            deleted = true;
        }
    }

    private void ensureInitialized() {
        if (id != null && entity == null) {
            entity = CommandContextUtil.getJobByteArrayEntityManager().findById(id);
            name = entity.getName();
        }
    }

    public boolean isDeleted() {
        return deleted;
    }

    /**
     * This makes a copy of this {@link JobByteArrayRef}: a new
     * {@link JobByteArrayRef} instance will be created, however with the same id,
     * name and {@link JobByteArrayEntity} instances.
     */
    public JobByteArrayRef copy() {
        JobByteArrayRef copy = new JobByteArrayRef();
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
