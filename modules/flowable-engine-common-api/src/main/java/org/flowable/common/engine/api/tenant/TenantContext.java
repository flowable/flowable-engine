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
package org.flowable.common.engine.api.tenant;

/**
 * Flowable Tenant context that can be implemented in different ways to hold and store the tenant information.
 *
 * @author Filip Hrisafov
 */
public interface TenantContext {

    /**
     * The id of the tenant.
     *
     * @return the id of the tenant
     */
    String getTenantId();

    /**
     * Changes the tenant id with the new value.
     *
     * @param tenantId the id of the tenant
     */
    void setTenantId(String tenantId);

    /**
     * Clears the last set value of the tenant.
     */
    void clearTenantId();

    /**
     * Flag indicating whether the tenant id is set.
     *
     * @return {@code true} if the tenant id can return a value for {@link #getTenantId()}, {@code false} otherwise
     */
    boolean isTenantIdSet();

}
