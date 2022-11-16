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

/**
 * A Map provider.
 *
 * <p>
 * The default MapProvider implementation is
 * {@link ConcurrentHashMapProviderImpl} which provides Javas default
 * {@link java.util.concurrent.ConcurrentHashMap} instances.
 * <p>
 * A custom implementation could provide some form of a distributed Concurrent
 * map using eg. Hazelcast.
 * 
 * <p>
 * Note that all implementations must return thread safe Maps!
 *
 * 
 * @author ikaakkola (Qvantel Finland Oy)
 */
public interface MapProvider {

    /**
     * Create a new Map instance
     *
     * @param <T>
     *            Type of values to store
     * @return The created Map instance
     */
    <T extends Object> Map<String, T> create();

    /**
     * Create a new Map instance
     *
     * @param <T>
     *            Type of values to store
     * @param initialCapacity
     *            the initial capacity for the Map
     * @param loadFactor
     *            the load factor for the Map
     * @return The created Map instance
     */
    <T> Map<String, T> create(int initialCapacity, float loadFactor);
}
