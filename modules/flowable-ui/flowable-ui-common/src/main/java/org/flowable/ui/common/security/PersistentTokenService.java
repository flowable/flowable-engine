package org.flowable.ui.common.security;

import org.flowable.idm.api.Token;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
public interface PersistentTokenService {

    Token getToken(String tokenId);

    Token invalidateCacheEntryAndGetToken(String tokenId, boolean invalidateCacheEntry);

    void delete(Token persistentToken);

    Token createToken(String userId, String remoteAddress, String userAgent);

}
