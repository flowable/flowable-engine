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
package org.flowable.common.engine.impl.persistence.deploy;

import java.util.Objects;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.repository.FlowableDefinition;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;

/**
 * Caches the id of the definition with a given key in the deployments of a parent deployment (or in a deployment), so
 * that repeated lookups do not need to query the database. A miss is only cached when
 * {@link AbstractEngineConfiguration#isCacheSameDeploymentDefinitionMisses()} is enabled, or when the caller always
 * caches misses. A cached miss applies to the tenant it was recorded for, a lookup for another tenant is treated as
 * not cached. A cached miss for a deployment that is deleted later stays in the cache until the cache limit evicts it.
 *
 * @param <T> the type of the definition
 */
public abstract class AbstractSameDeploymentDefinitionLookup<T extends FlowableDefinition> {

    protected static final String NO_DEFINITION_PREFIX = "__noDefinition__:";

    protected DeploymentCache<String> cache;
    protected AbstractEngineConfiguration engineConfiguration;

    public AbstractSameDeploymentDefinitionLookup(DeploymentCache<String> cache, AbstractEngineConfiguration engineConfiguration) {
        this.cache = cache;
        this.engineConfiguration = engineConfiguration;
    }

    public static String parentDeploymentKey(String parentDeploymentId, String key) {
        return "parent:" + parentDeploymentId + ":" + key;
    }

    public static String deploymentKey(String deploymentId, String key) {
        return "deployment:" + deploymentId + ":" + key;
    }

    /**
     * Returns the definition for the cache key, or null when there is none and the caller should use its fallback.
     */
    public T find(String cacheKey, String tenantId, boolean cacheMisses, Supplier<T> query) {
        String cachedValue = cache.get(cacheKey);
        String noDefinition = noDefinitionMarker(tenantId);
        if (noDefinition.equals(cachedValue)) {
            return null;
        }

        if (cachedValue != null && !cachedValue.startsWith(NO_DEFINITION_PREFIX)) {
            T definition = findDefinitionById(cachedValue);
            if (definition == null) {
                cache.remove(cacheKey);
            } else if (isSameTenant(definition.getTenantId(), tenantId)) {
                return definition;
            } else {
                return query.get();
            }
        }

        T definition = query.get();
        if (definition != null) {
            cache.add(cacheKey, definition.getId());
        } else if (cacheMisses) {
            cache.add(cacheKey, noDefinition);
        }
        return definition;
    }

    protected String noDefinitionMarker(String tenantId) {
        return NO_DEFINITION_PREFIX + StringUtils.defaultString(tenantId);
    }

    public boolean isCacheMisses() {
        return engineConfiguration.isCacheSameDeploymentDefinitionMisses();
    }

    public void evict(String cacheKey) {
        cache.remove(cacheKey);
    }

    protected boolean isSameTenant(String definitionTenantId, String tenantId) {
        return Objects.equals(StringUtils.defaultString(definitionTenantId), StringUtils.defaultString(tenantId));
    }

    /**
     * Returns the definition with the given id, or null when it no longer exists.
     */
    protected T findDefinitionById(String definitionId) {
        if (!isVerifyCachedDefinitions()) {
            T definition = findCachedDefinition(definitionId);
            if (definition != null) {
                return definition;
            }
        }
        return findPersistedDefinition(definitionId);
    }

    /**
     * Returns whether a cached definition id is always verified against the database.
     */
    protected boolean isVerifyCachedDefinitions() {
        return true;
    }

    /**
     * Returns the definition from the definition cache, or null when it is not cached or the engine always verifies.
     */
    protected T findCachedDefinition(String definitionId) {
        return null;
    }

    /**
     * Returns the definition from the database, or null when it does not exist.
     */
    protected abstract T findPersistedDefinition(String definitionId);

    public DeploymentCache<String> getCache() {
        return cache;
    }

    public void setCache(DeploymentCache<String> cache) {
        this.cache = cache;
    }
}
