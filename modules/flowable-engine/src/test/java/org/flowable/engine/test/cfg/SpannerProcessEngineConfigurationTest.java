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
package org.flowable.engine.test.cfg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.impl.db.DbIdGenerator;
import org.junit.jupiter.api.Test;

/**
 * Tests Spanner configuration defaults for {@link org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl}.
 */
public class SpannerProcessEngineConfigurationTest {

    @Test
    public void testInitIdGeneratorDefaultsToStrongUuidGeneratorForSpanner() {
        StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setDatabaseType(AbstractEngineConfiguration.DATABASE_TYPE_SPANNER);

        configuration.initIdGenerator();

        assertThat(configuration.getIdGenerator())
                .isNotNull()
                .isInstanceOf(StrongUuidGenerator.class);
    }

    @Test
    public void testInitIdGeneratorDefaultsToDbIdGeneratorForH2() {
        StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setDatabaseType(AbstractEngineConfiguration.DATABASE_TYPE_H2);
        configuration.initDefaultCommandConfig();

        configuration.initIdGenerator();

        assertThat(configuration.getIdGenerator())
                .isNotNull()
                .isInstanceOf(DbIdGenerator.class);
    }

    @Test
    public void testInitIdGeneratorPreservesCustomIdGeneratorForSpanner() {
        StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setDatabaseType(AbstractEngineConfiguration.DATABASE_TYPE_SPANNER);
        IdGenerator customIdGenerator = mock(IdGenerator.class);
        configuration.setIdGenerator(customIdGenerator);

        configuration.initIdGenerator();

        assertThat(configuration.getIdGenerator()).isSameAs(customIdGenerator);
    }
}
