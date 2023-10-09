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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.tenant.TenantContext;

/**
 * @author Filip Hrisafov
 */
public class ThreadLocalTenantContext implements TenantContext {

    protected final ThreadLocal<String> tenantId = ThreadLocal.withInitial(() -> {
        throw new FlowableException("Tenant value has not been set");
    });
    protected final ThreadLocal<Boolean> tenantIdSet = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Override
    public String getTenantId() {
        return tenantId.get();
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId.set(tenantId);
        this.tenantIdSet.set(true);
    }

    @Override
    public void clearTenantId() {
        tenantId.remove();
        tenantIdSet.remove();
    }

    @Override
    public boolean isTenantIdSet() {
        return tenantIdSet.get();
    }
}
