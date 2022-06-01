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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.InboundChannelModelCacheManager;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.model.InboundChannelModel;

public class DefaultInboundChannelModelCacheManager implements InboundChannelModelCacheManager {

    protected final EventRegistryEngineConfiguration engineConfiguration;
    protected final Map<CacheKey, String> cache = new HashMap<>();

    public DefaultInboundChannelModelCacheManager(EventRegistryEngineConfiguration engineConfiguration) {
        this.engineConfiguration = engineConfiguration;
    }

    @Override
    public boolean registerChannelModel(InboundChannelModel channelModel, ChannelDefinition channelDefinition) {
        String json = engineConfiguration.getChannelJsonConverter().convertToJson(channelModel);
        CacheKey key = new CacheKey(channelModel, channelDefinition);
        String registeredJson = cache.get(key);
        if (registeredJson == null || !registeredJson.equals(json)) {
            // When the cache does not contain mapping for the key
            // or the mapping is different, then we need to register the new mapping
            // and return true (that we did the registration)
            cache.put(key, json);
            return true;
        }

        return false;
    }

    @Override
    public void unregisterChannelModel(InboundChannelModel channelModel, ChannelDefinition channelDefinition) {
        cache.remove(new CacheKey(channelModel, channelDefinition));
    }

    @Override
    public void cleanChannelModels() {
        cache.clear();
    }

    protected static class CacheKey {

        protected final String modelType;
        protected final String modelKey;
        protected final String tenantId;

        protected CacheKey(InboundChannelModel model, ChannelDefinition definition) {
            this.modelType = model.getType();
            this.modelKey = model.getKey();
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
            return Objects.equals(modelType, cacheKey.modelType) && Objects.equals(modelKey, cacheKey.modelKey) && Objects.equals(tenantId, cacheKey.tenantId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(modelType, modelKey, tenantId);
        }
    }
}
