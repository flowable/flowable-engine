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
package org.flowable.common.engine.impl.scripting;

import java.util.HashMap;
import java.util.Map;

/**
 * Simplest implementation of a {@link Resolver} backed by a Map.
 */
public class MapResolver implements Resolver {

    protected Map<Object, Object> map;

    public MapResolver(Map<Object, Object> map) {
        this.map = new HashMap<>();
        this.map.putAll(map);
    }

    public MapResolver() {
        this.map = new HashMap<>();
    }

    @Override
    public boolean containsKey(Object key) {
        return map != null ? map.containsKey(key) : false;
    }

    @Override
    public Object get(Object key) {
        return map != null ? map.get(key) : null;
    }

    public MapResolver put(Object key, Object value) {
        this.map.put(key, value);
        return this;
    }
}
