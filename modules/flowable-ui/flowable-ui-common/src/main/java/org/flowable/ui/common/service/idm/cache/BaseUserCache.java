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
package org.flowable.ui.common.service.idm.cache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.flowable.ui.common.properties.FlowableCommonAppProperties;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

/**
 * Cache containing User objects to prevent too much DB-traffic (users exist separately from the Flowable tables, they need to be fetched afterward one by one to join with those entities).
 * <p>
 * TODO: This could probably be made more efficient with bulk getting. The Google cache impl allows this: override loadAll and use getAll() to fetch multiple entities.
 *
 * @author Frederik Heremans
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public abstract class BaseUserCache implements UserCache {

    protected final FlowableCommonAppProperties properties;

    protected LoadingCache<String, CachedUser> userCache;

    protected BaseUserCache(FlowableCommonAppProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    protected void initCache() {
        FlowableCommonAppProperties.Cache cache = properties.getCacheUsers();
        long userCacheMaxSize = cache.getMaxSize();
        long userCacheMaxAge = cache.getMaxAge();

        userCache = CacheBuilder.newBuilder().maximumSize(userCacheMaxSize)
            .expireAfterAccess(userCacheMaxAge, TimeUnit.SECONDS).recordStats().build(new CacheLoader<String, CachedUser>() {

                    @Override
                    public CachedUser load(final String userId) throws Exception {
                        return loadUser(userId);
                    }

                });
    }

    protected abstract CachedUser loadUser(String userId);

    @Override
    public void putUser(String userId, CachedUser cachedUser) {
        userCache.put(userId, cachedUser);
    }

    @Override
    public CachedUser getUser(String userId) {
        return getUser(userId, false, false, true); // always check validity by default
    }

    @Override
    public CachedUser getUser(String userId, boolean throwExceptionOnNotFound, boolean throwExceptionOnInactive, boolean checkValidity) {
        try {
            // The cache is a LoadingCache and will fetch the value itself
            CachedUser cachedUser = userCache.get(userId);
            return cachedUser;

        } catch (ExecutionException e) {
            return null;
        } catch (UncheckedExecutionException uee) {

            // Some magic with the exceptions is needed:
            // the exceptions like UserNameNotFound and Locked cannot
            // bubble up, since Spring security will react on them otherwise
            if (uee.getCause() instanceof RuntimeException) {
                RuntimeException runtimeException = (RuntimeException) uee.getCause();

                if (runtimeException instanceof UsernameNotFoundException) {
                    if (throwExceptionOnNotFound) {
                        throw runtimeException;
                    } else {
                        return null;
                    }
                }

                if (runtimeException instanceof LockedException) {
                    if (throwExceptionOnNotFound) {
                        throw runtimeException;
                    } else {
                        return null;
                    }
                }

            }
            throw uee;
        }
    }

    @Override
    public void invalidate(String userId) {
        userCache.invalidate(userId);
    }
}
