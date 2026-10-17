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
package org.flowable.cdi.test.impl.el;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.flowable.cdi.impl.el.CdiResolver;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.junit.jupiter.api.Test;

public class CdiResolverTest {

    @Test
    public void testConcurrentCallsDoNotShareTheElContext() throws Exception {
        Set<jakarta.el.ELContext> seenContexts = ConcurrentHashMap.newKeySet();
        CyclicBarrier bothCallsAreInside = new CyclicBarrier(2);
        CdiResolver resolver = new CdiResolver() {

            @Override
            protected jakarta.el.ELResolver getWrappedResolver() {
                return new RecordingResolver(seenContexts, bothCallsAreInside);
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Object> first = executor.submit(() -> resolver.getValue(new SimpleContext(), null, "bean"));
            Future<Object> second = executor.submit(() -> resolver.getValue(new SimpleContext(), null, "bean"));
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        // The resolver of the CDI container keeps state in the context, so two calls that run at the same time must not use the same one
        assertThat(seenContexts).hasSize(2);
    }

    @Test
    public void testElContextIsStoredOnTheFlowableContext() {
        Set<jakarta.el.ELContext> seenContexts = ConcurrentHashMap.newKeySet();
        CdiResolver resolver = new CdiResolver() {

            @Override
            protected jakarta.el.ELResolver getWrappedResolver() {
                return new RecordingResolver(seenContexts, null);
            }
        };

        SimpleContext flowableContext = new SimpleContext();
        resolver.getValue(flowableContext, null, "bean");
        resolver.getValue(flowableContext, null, "bean");
        assertThat(seenContexts).hasSize(1);

        resolver.getValue(new SimpleContext(), null, "bean");
        assertThat(seenContexts).hasSize(2);
    }

    protected static class RecordingResolver extends jakarta.el.ELResolver {

        protected final Set<jakarta.el.ELContext> seenContexts;
        protected final CyclicBarrier bothCallsAreInside;

        public RecordingResolver(Set<jakarta.el.ELContext> seenContexts, CyclicBarrier bothCallsAreInside) {
            this.seenContexts = seenContexts;
            this.bothCallsAreInside = bothCallsAreInside;
        }

        @Override
        public Object getValue(jakarta.el.ELContext context, Object base, Object property) {
            seenContexts.add(context);
            if (bothCallsAreInside != null) {
                try {
                    // Both calls are inside the resolver at the same time before either of them returns
                    bothCallsAreInside.await(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            return null;
        }

        @Override
        public Class<?> getType(jakarta.el.ELContext context, Object base, Object property) {
            return null;
        }

        @Override
        public void setValue(jakarta.el.ELContext context, Object base, Object property, Object value) {
        }

        @Override
        public boolean isReadOnly(jakarta.el.ELContext context, Object base, Object property) {
            return false;
        }

        @Override
        public Class<?> getCommonPropertyType(jakarta.el.ELContext context, Object base) {
            return null;
        }
    }
}
