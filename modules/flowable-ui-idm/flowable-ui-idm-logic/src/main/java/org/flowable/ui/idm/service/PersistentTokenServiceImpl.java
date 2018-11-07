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
package org.flowable.ui.idm.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Token;
import org.flowable.idm.api.User;
import org.flowable.ui.common.properties.FlowableCommonAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
@Service
@Transactional
public class PersistentTokenServiceImpl implements PersistentTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentTokenServiceImpl.class);

    private static final int DEFAULT_SERIES_LENGTH = 16;

    private static final int DEFAULT_TOKEN_LENGTH = 16;

    private SecureRandom random;

    @Autowired
    private FlowableCommonAppProperties properties;

    @Autowired
    private IdmIdentityService idmIdentityService;

    // Caching the persistent tokens to avoid hitting the database too often (eg when doing multiple requests at the same time)
    // (This happens a lot, when the page consists of multiple requests)
    private LoadingCache<String, Token> tokenCache;

    public PersistentTokenServiceImpl() {
        random = new SecureRandom();
    }

    @PostConstruct
    protected void initTokenCache() {
        FlowableCommonAppProperties.Cache cacheLoginUsers = properties.getCacheLoginUsers();
        long maxSize = cacheLoginUsers.getMaxSize();
        long maxAge = cacheLoginUsers.getMaxAge();
        tokenCache = CacheBuilder.newBuilder().maximumSize(maxSize).expireAfterWrite(maxAge, TimeUnit.SECONDS).recordStats()
                .build(new CacheLoader<String, Token>() {

                    public Token load(final String tokenId) throws Exception {
                        Token token = idmIdentityService.createTokenQuery().tokenId(tokenId).singleResult();
                        if (token != null) {
                            return token;
                        } else {
                            throw new PersistentTokenNotFoundException();
                        }
                    }

                });
    }

    @Override
    public Token saveAndFlush(Token token) {
        idmIdentityService.saveToken(token);
        return token;
    }

    @Override
    public void delete(Token token) {
        tokenCache.invalidate(token);
        idmIdentityService.deleteToken(token.getId());
    }

    @Override
    public Token getPersistentToken(String tokenId) {
        return getPersistentToken(tokenId, false);
    }

    @Override
    public Token getPersistentToken(String tokenId, boolean invalidateCacheEntry) {

        if (invalidateCacheEntry) {
            tokenCache.invalidate(tokenId);
        }

        try {
            return tokenCache.get(tokenId);
        } catch (ExecutionException e) {
            return null;
        } catch (UncheckedExecutionException e) {
            return null;
        }
    }

    private String generateSeriesData() {
        return generateRandomWithoutSlash(DEFAULT_SERIES_LENGTH);
    }

    private String generateTokenData() {
        return generateRandomWithoutSlash(DEFAULT_TOKEN_LENGTH);
    }

    private String generateRandomWithoutSlash(int size) {
        String data = generateRandom(size);
        while (data.contains("/")) {
            data = generateRandom(size);
        }
        return data;
    }

    private String generateRandom(int size) {
        byte[] s = new byte[size];
        random.nextBytes(s);
        return new String(Base64.getEncoder().encode(s));
    }

    @Override
    public Token createToken(User user, String remoteAddress, String userAgent) {

        Token token = idmIdentityService.newToken(generateSeriesData());
        token.setTokenValue(generateTokenData());
        token.setTokenDate(new Date());
        token.setIpAddress(remoteAddress);
        token.setUserAgent(userAgent);
        token.setUserId(user.getId());

        try {
            saveAndFlush(token);
            return token;
        } catch (DataAccessException e) {
            LOGGER.error("Failed to save persistent token ", e);
            return token;
        }
    }

    // Just helper exception class for handling null values
    private static class PersistentTokenNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

}
