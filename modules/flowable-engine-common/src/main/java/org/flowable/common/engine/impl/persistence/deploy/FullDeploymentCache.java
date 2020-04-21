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
package org.flowable.common.engine.impl.persistence.deploy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Default cache: keep everything in memory, without a limit.
 */
public class FullDeploymentCache<T> implements DeploymentCache<T> {

    protected Map<String, T> cache;

    /** Cache with no limit */
    public FullDeploymentCache() {
        this.cache = Collections.synchronizedMap(new HashMap<>());
    }

    @Override
    public T get(String id) {
        return cache.get(id);
    }

    @Override
    public void add(String id, T obj) {
        cache.put(id, obj);
    }

    @Override
    public void remove(String id) {
        cache.remove(id);
    }

    @Override
    public boolean contains(String id) {
        return cache.containsKey(id);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public Collection<T> getAll() {
        return cache.values();
    }

    @Override
    public int size() {
        return cache.size();
    }

}
