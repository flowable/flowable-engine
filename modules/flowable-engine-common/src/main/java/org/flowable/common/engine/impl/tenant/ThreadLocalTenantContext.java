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

import org.flowable.common.engine.api.tenant.TenantContext;

/**
 * @author Filip Hrisafov
 */
public class ThreadLocalTenantContext implements TenantContext {

    protected final ThreadLocal<Tenant> tenantId = new ThreadLocal<>();

    @Override
    public String getTenantId() {
        Tenant tenant = tenantId.get();
        return tenant != null ? tenant.tenantId() : null;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId.set(new Tenant(tenantId));
    }

    @Override
    public void clearTenantId() {
        tenantId.remove();
    }

    @Override
    public boolean isTenantIdSet() {
        return tenantId.get() != null;
    }

    protected record Tenant(String tenantId) {

    }
}
