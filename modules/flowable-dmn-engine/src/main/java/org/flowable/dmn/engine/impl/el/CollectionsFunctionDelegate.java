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
package org.flowable.dmn.engine.impl.el;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import org.flowable.common.engine.api.delegate.FlowableMultiFunctionDelegate;
import org.flowable.dmn.engine.impl.el.util.CollectionUtil;

/**
 * Collections functions mapper that can be used in EL expressions. It provides the following expressions:
 * <ul>
 *     <li><code>collection:allOf(collection, value)</code></li>
 *     <li><code>collection:anyOf(collection, value)</code></li>
 *     <li><code>collection:noneOf(collection, value)</code></li>
 *     <li><code>collection:notAllOf(collection, value)</code></li>
 *
 *     <li><code>collection:containsAny(collection, value)</code></li>
 *     <li><code>collection:contains(collection, value)</code></li>
 *     <li><code>collection:notContainsAny(collection, value)</code></li>
 *     <li><code>collection:notContains(collection, value)</code></li>
 * </ul>
 * @author Filip Hrisafov
 */
public class CollectionsFunctionDelegate implements FlowableMultiFunctionDelegate {

    @Override
    public String prefix() {
        return "collection";
    }

    @Override
    public Collection<String> localNames() {
        return List.of(
                "allOf",
                "anyOf",
                "noneOf",
                "notAllOf",

                // deprecated
                "containsAny",
                "contains",
                "notContainsAny",
                "notContains"
        );
    }

    @Override
    public Method functionMethod(String prefix, String localName) throws NoSuchMethodException {
        return CollectionUtil.class.getDeclaredMethod(localName, Object.class, Object.class);
    }
}
