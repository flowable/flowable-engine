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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.Engine;
import org.junit.jupiter.api.Test;

class EngineTestCacheTest {

    static class TestEngine implements Engine {

        final String name;
        boolean closed;
        RuntimeException throwOnClose;

        TestEngine(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void close() {
            if (throwOnClose != null) {
                throw throwOnClose;
            }
            this.closed = true;
        }
    }

    @Test
    void getOrCreateBuildsOnceAndReturnsCachedInstance() {
        EngineTestCache<TestEngine> cache = new EngineTestCache<>(2);
        List<String> built = new ArrayList<>();

        TestEngine first = cache.getOrCreate("a", key -> {
            built.add(key);
            return new TestEngine(key);
        });
        TestEngine second = cache.getOrCreate("a", key -> {
            built.add(key);
            return new TestEngine(key);
        });

        assertThat(second).isSameAs(first);
        assertThat(built).containsExactly("a");
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    void evictsAndClosesLeastRecentlyUsedWhenOverMaxSize() {
        EngineTestCache<TestEngine> cache = new EngineTestCache<>(2);

        TestEngine a = cache.getOrCreate("a", TestEngine::new);
        TestEngine b = cache.getOrCreate("b", TestEngine::new);
        // access "a" so "b" becomes the least-recently-used
        cache.getOrCreate("a", TestEngine::new);
        TestEngine c = cache.getOrCreate("c", TestEngine::new);

        assertThat(cache.size()).isEqualTo(2);
        assertThat(b.closed).isTrue();
        assertThat(a.closed).isFalse();
        assertThat(c.closed).isFalse();
    }

    @Test
    void closeAllClosesEveryEngineAndEmptiesCache() {
        EngineTestCache<TestEngine> cache = new EngineTestCache<>(4);
        TestEngine a = cache.getOrCreate("a", TestEngine::new);
        TestEngine b = cache.getOrCreate("b", TestEngine::new);

        cache.closeAll();

        assertThat(a.closed).isTrue();
        assertThat(b.closed).isTrue();
        assertThat(cache.size()).isZero();
    }

    @Test
    void defaultMaxSizeIsUsedWhenSystemPropertyAbsent() {
        // property must not be set in this JVM for the assertion to be meaningful
        assertThat(System.getProperty(EngineTestCache.MAX_SIZE_PROPERTY)).isNull();
        EngineTestCache<TestEngine> cache = new EngineTestCache<>();

        for (int i = 0; i < EngineTestCache.DEFAULT_MAX_SIZE + 5; i++) {
            cache.getOrCreate("key-" + i, TestEngine::new);
        }

        assertThat(cache.size()).isEqualTo(EngineTestCache.DEFAULT_MAX_SIZE);
    }

    @Test
    void evictionToleratesExceptionFromClosingEldestEngine() {
        EngineTestCache<TestEngine> cache = new EngineTestCache<>(2);

        TestEngine a = cache.getOrCreate("a", TestEngine::new);
        TestEngine b = cache.getOrCreate("b", TestEngine::new);
        // Make b throw on close
        b.throwOnClose = new RuntimeException("boom");

        // access "a" so "b" becomes the least-recently-used and will be evicted
        cache.getOrCreate("a", TestEngine::new);
        // This should not throw even though b.close() throws
        TestEngine c = cache.getOrCreate("c", TestEngine::new);

        // Verify eviction still happened (b was removed) and cache is at maxSize
        assertThat(cache.size()).isEqualTo(2);
        // a and c should be in the cache (b was evicted despite throwing)
        assertThat(a.closed).isFalse();
        assertThat(c.closed).isFalse();
    }
}
