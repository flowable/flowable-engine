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
package org.flowable.spring.boot;

import java.util.Collections;
import java.util.Set;

import org.flowable.common.engine.api.Engine;
import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDependsOnDatabaseInitializationDetector;
import org.springframework.core.env.Environment;

/**
 * {@link org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitializationDetector}
 * marking all Flowable {@link Engine} beans as depending on database initialization, so Spring
 * Boot orders them after Flyway, Liquibase or spring.sql.init have run.
 *
 * <p>Can be opted out of by setting {@code flowable.depends-on-database-initialization-detection}
 * to {@code false}, mirroring the JPA detector's opt-out behavior.
 *
 * @author Nikhil Bharadwaj Ramashasthri
 */
class FlowableDependsOnDatabaseInitializationDetector extends AbstractBeansOfTypeDependsOnDatabaseInitializationDetector {

    private final Environment environment;

    FlowableDependsOnDatabaseInitializationDetector(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected Set<Class<?>> getDependsOnDatabaseInitializationBeanTypes() {
        boolean detectionEnabled = environment
                .getProperty("flowable.depends-on-database-initialization-detection", Boolean.class, true);
        return detectionEnabled ? Collections.singleton(Engine.class) : Collections.emptySet();
    }
}
