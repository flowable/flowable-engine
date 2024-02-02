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
package org.flowable.common.engine.impl.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.common.engine.api.tenant.TenantContext;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class ThreadLocalTenantContextTest {

    protected TenantContext underTest = new ThreadLocalTenantContext();

    @Test
    void getTenantId() {
        assertThat(underTest.getTenantId()).isNull();

        underTest.setTenantId("acme");
        assertThat(underTest.getTenantId()).isEqualTo("acme");

        underTest.setTenantId("muppets");
        assertThat(underTest.getTenantId()).isEqualTo("muppets");

        underTest.clearTenantId();
        assertThat(underTest.getTenantId()).isNull();
    }

    @Test
    void isTenantIdSet() {
        assertThat(underTest.isTenantIdSet()).isFalse();

        underTest.setTenantId("flowable");
        assertThat(underTest.isTenantIdSet()).isTrue();

        underTest.clearTenantId();
        assertThat(underTest.isTenantIdSet()).isFalse();

        underTest.setTenantId(null);
        assertThat(underTest.isTenantIdSet()).isTrue();
    }
}
