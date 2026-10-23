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
package org.flowable.variable.service.impl.types;

/**
 * Controls how the {@link SerializableType} (Java serialization of variable values) behaves.
 *
 * @author Filip Hrisafov
 */
public enum SerializableVariableTypeMode {

    /**
     * Serializable variables can be read and written.
     */
    ENABLED,

    /**
     * Existing serializable variables can be read, but no new serializable values can be stored.
     * Useful for migrating existing data away from Java serialization.
     */
    READ_ONLY,

    /**
     * Serializable variables can neither be read nor written.
     * Existing variables of this type can still be loaded (e.g. for deletion), but accessing their value fails.
     */
    DISABLED;

    public boolean isReadable() {
        return this != DISABLED;
    }

    public boolean isWritable() {
        return this == ENABLED;
    }
}
