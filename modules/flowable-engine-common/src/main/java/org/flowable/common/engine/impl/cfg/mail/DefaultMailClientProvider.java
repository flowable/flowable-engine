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
package org.flowable.common.engine.impl.cfg.mail;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.mail.common.api.client.FlowableMailClient;
import org.flowable.mail.common.api.client.MailClientProvider;

/**
 * Default implementation of {@link MailClientProvider} that resolves mail clients from
 * a static map keyed by tenant identifier, with a fallback to a default mail client.
 *
 * @author Valentin Zickner
 */
public class DefaultMailClientProvider implements MailClientProvider {

    protected FlowableMailClient defaultMailClient;
    protected final Map<String, FlowableMailClient> tenantMailClients;

    public DefaultMailClientProvider() {
        this.tenantMailClients = new HashMap<>();
    }

    @Override
    public FlowableMailClient getMailClient(String tenantId) {
        if (StringUtils.isNotBlank(tenantId)) {
            FlowableMailClient client = tenantMailClients.get(tenantId);
            if (client != null) {
                return client;
            }
        }
        return defaultMailClient;
    }

    public FlowableMailClient getDefaultMailClient() {
        return defaultMailClient;
    }

    public void setDefaultMailClient(FlowableMailClient defaultMailClient) {
        this.defaultMailClient = defaultMailClient;
    }

    public Map<String, FlowableMailClient> getMailClients() {
        return tenantMailClients;
    }

}
