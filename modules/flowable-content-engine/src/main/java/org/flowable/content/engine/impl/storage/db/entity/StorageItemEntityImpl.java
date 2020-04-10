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

package org.flowable.content.engine.impl.storage.db.entity;

import java.io.Serializable;
import org.flowable.content.engine.impl.persistence.entity.AbstractContentEngineNoRevisionEntity;

/**
 * @author Jorge Moraleda
 */
public class StorageItemEntityImpl extends AbstractContentEngineNoRevisionEntity implements StorageItemEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected byte[] bytes;

    public StorageItemEntityImpl() {}

    @Override
    public String toString() {
        return "StorageEntity[id=" + id + "]";
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public Object getPersistentState() {
        return StorageItemEntityImpl.class;
    }

}
