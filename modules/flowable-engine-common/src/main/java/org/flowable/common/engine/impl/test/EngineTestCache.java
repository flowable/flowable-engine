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
package org.flowable.common.engine.impl.test;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.flowable.common.engine.api.Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A small bounded, least-recently-used cache of {@link Engine engines} for tests, keyed by configuration resource.
 *
 * <p>This is a deliberately rudimentary analog of Spring's {@code ContextCache}: when more than {@code maxSize}
 * engines are cached, the least-recently-used engine is {@link Engine#close() closed} and evicted. It keeps the
 * number of simultaneously-live engines bounded across a large test suite that runs in a single JVM.</p>
 *
 * <p>The maximum size defaults to {@link #DEFAULT_MAX_SIZE} and can be overridden with the
 * {@code flowable.test.engine.cache.maxSize} system property.</p>
 *
 * <p><b>Note on the bound:</b> a test helper may hold a direct reference to an engine it obtained from this cache for
 * the duration of a test class. Eviction closes the least-recently-used engine, so the configured size must comfortably
 * exceed the maximum number of distinct configuration resources that can be live within any single test class.
 * The {@link #DEFAULT_MAX_SIZE default} is ample for the usual one-configuration-per-class tests; setting it very low
 * (e.g. 1) is only safe for suites where each class uses a single configuration resource.</p>
 *
 * @author Filip Hrisafov
 */
public class EngineTestCache<T extends Engine> {

    public static final int DEFAULT_MAX_SIZE = 32;
    public static final String MAX_SIZE_PROPERTY = "flowable.test.engine.cache.maxSize";

    private static final Logger LOGGER = LoggerFactory.getLogger(EngineTestCache.class);

    protected final int maxSize;
    protected final Map<String, T> engines;

    public EngineTestCache() {
        this(resolveMaxSize());
    }

    public EngineTestCache(int maxSize) {
        this.maxSize = maxSize;
        // accessOrder = true -> iteration order is least-recently-accessed first, so the eldest entry is the LRU one.
        // Wrapped in a synchronized map (as Spring's DefaultContextCache does) since an access-ordered LinkedHashMap
        // mutates its structure even on get(), so concurrent access would otherwise corrupt it.
        this.engines = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true));
    }

    /**
     * Returns the cached engine for the given configuration resource, building and caching it with the supplied
     * builder on a miss. Accessing an entry marks it most-recently-used; a miss that would exceed the maximum size
     * first evicts and closes the least-recently-used engine.
     */
    public T getOrCreate(String key, Function<String, T> builder) {
        T engine = engines.get(key);
        if (engine == null) {
            evictLeastRecentlyUsedIfNecessary();
            engine = builder.apply(key);
            engines.put(key, engine);
        }
        return engine;
    }

    /**
     * Evicts and closes the least-recently-used engine when the cache is full, before a new entry is added.
     * Mirrors Spring's {@code DefaultContextCache}: the LRU entry is the first key in an access-ordered map.
     */
    protected void evictLeastRecentlyUsedIfNecessary() {
        if (engines.size() >= maxSize) {
            Iterator<String> iterator = engines.keySet().iterator();
            if (iterator.hasNext()) {
                String lruKey = iterator.next();
                T evicted = engines.remove(lruKey);
                if (evicted != null) {
                    LOGGER.debug("Evicting and closing least-recently-used test engine '{}'", lruKey);
                    try {
                        evicted.close();
                    } catch (Exception e) {
                        LOGGER.warn("Error while closing evicted test engine '{}'", lruKey, e);
                    }
                }
            }
        }
    }

    /** Closes every cached engine (ignoring individual close failures) and empties the cache. */
    public void closeAll() {
        // Iterating a synchronized map requires holding its monitor for the duration of the iteration.
        synchronized (engines) {
            for (T engine : engines.values()) {
                try {
                    engine.close();
                } catch (Exception e) {
                    LOGGER.warn("Error while closing test engine '{}'", engine.getName(), e);
                }
            }
            engines.clear();
        }
    }

    public int size() {
        return engines.size();
    }

    protected static int resolveMaxSize() {
        String configured = System.getProperty(MAX_SIZE_PROPERTY);
        if (configured != null) {
            try {
                return Integer.parseInt(configured.trim());
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid value '{}' for system property {}, falling back to default {}", configured, MAX_SIZE_PROPERTY, DEFAULT_MAX_SIZE);
            }
        }
        return DEFAULT_MAX_SIZE;
    }
}
