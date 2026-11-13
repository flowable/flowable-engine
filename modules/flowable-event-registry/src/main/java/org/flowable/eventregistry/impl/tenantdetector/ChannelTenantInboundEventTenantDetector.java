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
package org.flowable.eventregistry.impl.tenantdetector;

import org.flowable.eventregistry.api.InboundEventTenantDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The tenant detector of an inbound channel that is deployed in a specific tenant. Such a channel only produces events for its own tenant:
 * the tenant detected by the tenant detection of the channel is replaced by the tenant of the channel. Without this, an event without
 * a tenant would be correlated with the event subscriptions of all tenants, and an event of another tenant would reach that tenant.
 */
public class ChannelTenantInboundEventTenantDetector<T> implements InboundEventTenantDetector<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelTenantInboundEventTenantDetector.class);

    protected String channelKey;
    protected String channelTenantId;
    protected InboundEventTenantDetector<T> channelTenantDetector;

    /**
     * @param channelTenantDetector the tenant detector of the channel model, which can be {@code null} when the channel has no tenant detection
     */
    public ChannelTenantInboundEventTenantDetector(String channelKey, String channelTenantId, InboundEventTenantDetector<T> channelTenantDetector) {
        this.channelKey = channelKey;
        this.channelTenantId = channelTenantId;
        this.channelTenantDetector = channelTenantDetector;
    }

    @Override
    public String detectTenantId(T payload) {
        if (channelTenantDetector != null) {
            String detectedTenantId = channelTenantDetector.detectTenantId(payload);
            if (!channelTenantId.equals(detectedTenantId)) {
                LOGGER.warn("Tenant {} detected for an event on channel {} is not the tenant of the channel. The event is processed for tenant {}",
                        detectedTenantId, channelKey, channelTenantId);
            }
        }

        return channelTenantId;
    }

    public String getChannelKey() {
        return channelKey;
    }

    public String getChannelTenantId() {
        return channelTenantId;
    }

    public InboundEventTenantDetector<T> getChannelTenantDetector() {
        return channelTenantDetector;
    }
}
