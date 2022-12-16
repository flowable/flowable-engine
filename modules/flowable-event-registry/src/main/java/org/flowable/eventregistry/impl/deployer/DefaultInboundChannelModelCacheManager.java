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
package org.flowable.eventregistry.impl.deployer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.InboundChannelModelCacheManager;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.model.InboundChannelModel;

public class DefaultInboundChannelModelCacheManager implements InboundChannelModelCacheManager {

    protected final EventRegistryEngineConfiguration engineConfiguration;
    protected final Map<CacheKey, CacheValue> cache = new HashMap<>();

    public DefaultInboundChannelModelCacheManager(EventRegistryEngineConfiguration engineConfiguration) {
        this.engineConfiguration = engineConfiguration;
    }

    @Override
    public ChannelRegistration registerChannelModel(InboundChannelModel channelModel, ChannelDefinition channelDefinition) {
        String json = engineConfiguration.getChannelJsonConverter().convertToJson(channelModel);
        CacheKey key = new CacheKey(channelDefinition);
        CacheValue cacheValue = cache.get(key);
        if (cacheValue == null) {
            // When the cache does not contain mapping for the key
            // then we need to register the new mapping
            // and return true (that we did the registration)
            cache.put(key, new CacheValue(json, channelDefinition));
            return new ChannelRegistrationImpl(true, null, key);
        } else if (cacheValue.version <= channelDefinition.getVersion()) {
            // When the latest version of the cache is less than the channel being deployed
            // then we need to check the json and update the cache
            cache.put(key, new CacheValue(json, channelDefinition));

            // When the registered json is different of the newer json then we should not register the channel model
            return new ChannelRegistrationImpl(!cacheValue.json.equals(json), new CacheRegisteredChannel(cacheValue), key);
        }

        return new ChannelRegistrationImpl(false, new CacheRegisteredChannel(cacheValue), key);
    }

    @Override
    public void unregisterChannelModel(InboundChannelModel channelModel, ChannelDefinition channelDefinition) {
        cache.remove(new CacheKey(channelDefinition));
    }

    @Override
    public void cleanChannelModels() {
        cache.clear();
    }

    @Override
    public RegisteredChannel findRegisteredChannel(ChannelDefinition channelDefinition) {
        CacheValue cacheValue = cache.get(new CacheKey(channelDefinition));
        return cacheValue != null ? new CacheRegisteredChannel(cacheValue) : null;
    }

    @Override
    public Collection<RegisteredChannel> getRegisteredChannels() {
        if (cache.isEmpty()) {
            return Collections.emptyList();
        }
        List<RegisteredChannel> registeredChannels = new ArrayList<>(cache.size());
        for (Map.Entry<CacheKey, CacheValue> entry : cache.entrySet()) {
            registeredChannels.add(new CacheRegisteredChannel(entry.getValue()));
        }

        return registeredChannels;
    }

    protected static class CacheKey {

        protected final String modelKey;
        protected final String tenantId;

        protected CacheKey(ChannelDefinition definition) {
            this.modelKey = definition.getKey();
            this.tenantId = definition.getTenantId();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CacheKey cacheKey = (CacheKey) o;
            return Objects.equals(modelKey, cacheKey.modelKey) && Objects.equals(tenantId, cacheKey.tenantId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(modelKey, tenantId);
        }
    }

    protected static class CacheValue {

        protected final String json;
        protected final int version;
        protected final String definitionId;

        public CacheValue(String json, ChannelDefinition definition) {
            this.json = json;
            this.version = definition.getVersion();
            this.definitionId = definition.getId();
        }
    }

    protected static class CacheRegisteredChannel implements RegisteredChannel {

        protected final CacheValue value;

        protected CacheRegisteredChannel(CacheValue value) {
            this.value = value;
        }

        @Override
        public int getChannelDefinitionVersion() {
            return value.version;
        }

        @Override
        public String getChannelDefinitionId() {
            return value.definitionId;
        }
    }

    protected class ChannelRegistrationImpl implements ChannelRegistration {

        protected final boolean registered;
        protected final CacheRegisteredChannel previousChannel;
        protected final CacheKey cacheKey;

        public ChannelRegistrationImpl(boolean registered, CacheRegisteredChannel previousChannel, CacheKey cacheKey) {
            this.registered = registered;
            this.previousChannel = previousChannel;
            this.cacheKey = cacheKey;
        }

        @Override
        public boolean registered() {
            return registered;
        }

        @Override
        public RegisteredChannel previousChannel() {
            return previousChannel;
        }

        @Override
        public void rollback() {
            CacheValue cacheValue = previousChannel != null ? previousChannel.value : null;
            if (cacheValue == null) {
                cache.remove(cacheKey);
            } else {
                cache.put(cacheKey, cacheValue);
            }
        }
    }
}
