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
package org.flowable.engine.data.inmemory.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link MapProvider} providing non-distributed Concurrent HashMaps based on
 * the standard Java {@link ConcurrentHashMap}
 *
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class ConcurrentHashMapProviderImpl implements MapProvider {

    @Override
    public <T> Map<String, T> create() {
        return new ConcurrentHashMap<>();
    }

    @Override
    public <T> Map<String, T> create(int initialCapacity, float loadFactor) {
        return new ConcurrentHashMap<>(initialCapacity, loadFactor);
    }
}
