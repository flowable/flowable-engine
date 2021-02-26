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
package org.flowable.ui.common.security;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.idm.api.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
public abstract class BasePersistentTokenService implements PersistentTokenService, InitializingBean {

    private static final int DEFAULT_SERIES_LENGTH = 16;

    private static final int DEFAULT_TOKEN_LENGTH = 16;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected SecureRandom random;

    protected long maxUserSize = 2058L;
    protected Duration maxAge = Duration.ofSeconds(30);

    // Caching the persistent tokens to avoid hitting the database too often (eg when doing multiple requests at the same time)
    // (This happens a lot, when the page consists of multiple requests)
    private LoadingCache<String, Token> tokenCache;

    public BasePersistentTokenService() {
        random = new SecureRandom();
    }

    @Override
    public void afterPropertiesSet() {
        tokenCache = CacheBuilder.newBuilder()
                .maximumSize(maxUserSize)
                .expireAfterWrite(maxAge)
                .recordStats()
                .build(new CacheLoader<String, Token>() {

                    @Override
                    public Token load(String s) throws Exception {
                        return loadToken(s);
                    }
                });
    }

    protected abstract Token loadToken(String tokenId);

    @Override
    public void delete(Token token) {
        tokenCache.invalidate(token.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Token getToken(String tokenId) {
        try {
            return tokenCache.get(tokenId);
        } catch (Exception e) {
            if (e instanceof UncheckedExecutionException && e.getCause() instanceof FlowableObjectNotFoundException) {
                logger.info("Token id {} does not exist in cache.", tokenId);
            } else {
                logger.error("Error loading token id {} from cache", tokenId, e);
            }
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Token invalidateCacheEntryAndGetToken(String tokenId, boolean invalidateCacheEntry) {
        if (invalidateCacheEntry) {
            tokenCache.invalidate(tokenId);
        }

        return getToken(tokenId);
    }

    protected String generateSeriesData() {
        return generateRandomWithoutSlash(DEFAULT_SERIES_LENGTH);
    }

    protected String generateTokenData() {
        return generateRandomWithoutSlash(DEFAULT_TOKEN_LENGTH);
    }

    protected String generateRandomWithoutSlash(int size) {
        String data = generateRandom(size);
        while (data.contains("/")) {
            data = generateRandom(size);
        }
        return data;
    }

    protected String generateRandom(int size) {
        byte[] s = new byte[size];
        random.nextBytes(s);
        return new String(Base64.getEncoder().encode(s));
    }

    public long getMaxUserSize() {
        return maxUserSize;
    }

    public void setMaxUserSize(long maxUserSize) {
        this.maxUserSize = maxUserSize;
    }

    public Duration getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Duration maxAge) {
        this.maxAge = maxAge;
    }
}
