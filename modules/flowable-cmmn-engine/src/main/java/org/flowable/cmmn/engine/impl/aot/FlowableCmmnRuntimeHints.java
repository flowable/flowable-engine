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
package org.flowable.cmmn.engine.impl.aot;

import org.flowable.common.engine.impl.aot.FlowableMyBatisResourceHintsRegistrar;
import org.flowable.variable.service.impl.QueryVariableValue;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * @author Filip Hrisafov
 */
public class FlowableCmmnRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        ResourceHints resourceHints = hints.resources();
        FlowableMyBatisResourceHintsRegistrar.registerMappingResources("org/flowable/cmmn/db/mapping", hints, classLoader);
        resourceHints.registerPattern("org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml");
        resourceHints.registerPattern("org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog-crdb.xml");
        resourceHints.registerPattern("org/flowable/impl/cmmn/parser/*.xsd");

        hints.reflection()
                .registerType(QueryVariableValue.class, MemberCategory.INVOKE_PUBLIC_METHODS);
    }
}
