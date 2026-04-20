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

import java.util.Collections;
import java.util.List;

/**
 * @author Filip Hrisafov
 */
public class CompositeResolver implements Resolver {

    protected final List<Resolver> resolvers;

    public CompositeResolver(List<Resolver> resolvers) {
        this.resolvers = resolvers == null ? Collections.emptyList() : resolvers;
    }

    @Override
    public boolean containsKey(Object key) {
        for (Resolver resolver : resolvers) {
            if (resolver.containsKey(key)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Object get(Object key) {
        for (Resolver resolver : resolvers) {
            if (resolver.containsKey(key)) {
                return resolver.get(key);
            }
        }
        return null;
    }
}
