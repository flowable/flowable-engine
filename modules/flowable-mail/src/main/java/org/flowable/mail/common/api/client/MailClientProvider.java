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
package org.flowable.mail.common.api.client;

/**
 * Provider for dynamically resolving {@link FlowableMailClient} instances.
 * <p>
 * This allows mail client resolution to be dynamic rather than statically configured at engine startup.
 * For example, this can be used to support dynamically added tenants or configuration changes at runtime.
 * <p>
 * Implementations must be thread-safe, as this provider may be called concurrently from multiple threads.
 *
 * @author Valentin Zickner
 */
public interface MailClientProvider {

    /**
     * Returns the mail client for the given tenant identifier.
     *
     * @param tenantId the tenant identifier, or {@code null} for the default (non-tenant) case
     * @return the mail client, or {@code null} if this provider cannot resolve one
     */
    FlowableMailClient getMailClient(String tenantId);
}
