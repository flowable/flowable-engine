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
package org.flowable.common.engine.impl.aot;

import org.flowable.common.engine.impl.persistence.entity.ByteArrayRefTypeHandler;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * @author Filip Hrisafov
 */
public class FlowableCommonRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        ResourceHints resourceHints = hints.resources();
        resourceHints.registerResourceBundle("org.flowable.common.engine.impl.de.odysseus.el.misc.LocalStrings");
        // If we can detect which DB is being used we can perhaps register only the appropriate DB file
        resourceHints.registerPattern("org/flowable/common/db/properties/*.properties");
        FlowableSqlResourceHintsRegistrar.registerSqlResources("org/flowable/common/db", resourceHints);

        hints.reflection()
                .registerType(ByteArrayRefTypeHandler.class, MemberCategory.values());
    }
}
