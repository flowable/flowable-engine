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

package org.flowable.idm.engine.test.api.identity;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.idm.api.Token;
import org.flowable.idm.api.TokenQuery;
import org.flowable.idm.engine.impl.persistence.entity.TokenEntity;
import org.flowable.idm.engine.test.PluggableFlowableIdmTestCase;

/**
 * @author Tijs Rademakers
 */
public class TokenQueryTest extends PluggableFlowableIdmTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Calendar tokenCal = new GregorianCalendar();
        tokenCal.set(2015, 1, 1, 0, 0, 0);
        createToken("1111", "aaaa", tokenCal.getTime(), "127.0.0.1", "chrome", "user1", null);

        tokenCal.set(2016, 1, 1, 0, 0, 0);
        createToken("2222", "bbbb", tokenCal.getTime(), "128.0.0.2", "firefox", "user2", null);

        tokenCal.set(2017, 1, 1, 0, 0, 0);
        createToken("3333", "cccc", tokenCal.getTime(), "129.0.0.1", "firefox2", "bla3", "test");
    }

    private Token createToken(String id, String tokenValue, Date tokenDate, String ipAddress,
            String userAgent, String userId, String tokenData) {

        Token token = idmIdentityService.newToken(id);
        token.setTokenValue(tokenValue);
        token.setTokenDate(tokenDate);
        token.setIpAddress(ipAddress);
        token.setUserAgent(userAgent);
        token.setUserId(userId);
        token.setTokenData(tokenData);
        idmIdentityService.saveToken(token);
        return token;
    }

    @Override
    protected void tearDown() throws Exception {
        idmIdentityService.deleteToken("1111");
        idmIdentityService.deleteToken("2222");
        idmIdentityService.deleteToken("3333");

        super.tearDown();
    }

    public void testQueryByNoCriteria() {
        TokenQuery query = idmIdentityService.createTokenQuery();
        verifyQueryResults(query, 3);
    }

    public void testQueryById() {
        TokenQuery query = idmIdentityService.createTokenQuery().tokenId("1111");
        verifyQueryResults(query, 1);
    }

    public void testQueryByInvalidId() {
        TokenQuery query = idmIdentityService.createTokenQuery().tokenId("invalid");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createTokenQuery().tokenId(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByTokenValue() {
        TokenQuery query = idmIdentityService.createTokenQuery().tokenValue("aaaa");
        verifyQueryResults(query, 1);

        Token result = query.singleResult();
        assertEquals("aaaa", result.getTokenValue());
    }

    public void testQueryByInvalidTokenValue() {
        TokenQuery query = idmIdentityService.createTokenQuery().tokenValue("invalid");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createTokenQuery().tokenValue(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByTokenDateBefore() {
        Calendar queryCal = new GregorianCalendar(2015, 6, 1, 0, 0, 0);
        TokenQuery query = idmIdentityService.createTokenQuery().tokenDateBefore(queryCal.getTime());
        verifyQueryResults(query, 1);

        Token result = query.singleResult();
        Calendar tokenCal = new GregorianCalendar();
        tokenCal.setTime(result.getTokenDate());
        assertEquals(2015, tokenCal.get(Calendar.YEAR));
        assertEquals(1, tokenCal.get(Calendar.MONTH));
    }

    public void testQueryByTokenDateAfter() {
        Calendar queryCal = new GregorianCalendar(2016, 6, 1, 0, 0, 0);
        TokenQuery query = idmIdentityService.createTokenQuery().tokenDateAfter(queryCal.getTime());
        verifyQueryResults(query, 1);

        Token result = query.singleResult();
        Calendar tokenCal = new GregorianCalendar();
        tokenCal.setTime(result.getTokenDate());
        assertEquals(2017, tokenCal.get(Calendar.YEAR));
        assertEquals(1, tokenCal.get(Calendar.MONTH));
    }

    public void testQueryByIpAddress() {
        TokenQuery query = idmIdentityService.createTokenQuery().ipAddress("127.0.0.1");
        verifyQueryResults(query, 1);

        Token result = query.singleResult();
        assertEquals("127.0.0.1", result.getIpAddress());
    }

    public void testQueryByInvalidIpAddress() {
        TokenQuery query = idmIdentityService.createTokenQuery().ipAddress("invalid");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createTokenQuery().ipAddress(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByIpAddressLike() {
        TokenQuery query = idmIdentityService.createTokenQuery().ipAddressLike("%0.0.1%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createTokenQuery().ipAddressLike("129.%");
        verifyQueryResults(query, 1);
    }

    public void testQueryByInvalidIpAddressLike() {
        TokenQuery query = idmIdentityService.createTokenQuery().ipAddressLike("%invalid%");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createTokenQuery().ipAddressLike(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByUserAgent() {
        TokenQuery query = idmIdentityService.createTokenQuery().userAgent("chrome");
        verifyQueryResults(query, 1);

        Token result = query.singleResult();
        assertEquals("chrome", result.getUserAgent());
    }

    public void testQueryByInvalidUserAgent() {
        TokenQuery query = idmIdentityService.createTokenQuery().userAgent("invalid");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createTokenQuery().userAgent(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByUserAgentLike() {
        TokenQuery query = idmIdentityService.createTokenQuery().userAgentLike("%fire%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createTokenQuery().userAgentLike("ch%");
        verifyQueryResults(query, 1);
    }

    public void testQueryByInvalidUserAgentLike() {
        TokenQuery query = idmIdentityService.createTokenQuery().userAgentLike("%invalid%");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createTokenQuery().userAgentLike(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByUserId() {
        TokenQuery query = idmIdentityService.createTokenQuery().userId("user1");
        verifyQueryResults(query, 1);

        Token result = query.singleResult();
        assertEquals("user1", result.getUserId());
    }

    public void testQueryByInvalidUserId() {
        TokenQuery query = idmIdentityService.createTokenQuery().userId("invalid");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createTokenQuery().userId(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByUserIdLike() {
        TokenQuery query = idmIdentityService.createTokenQuery().userIdLike("%user%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createTokenQuery().userIdLike("bla%");
        verifyQueryResults(query, 1);
    }

    public void testQueryByInvalidUserIdLike() {
        TokenQuery query = idmIdentityService.createTokenQuery().userIdLike("%invalid%");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createTokenQuery().userIdLike(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByTokenData() {
        TokenQuery query = idmIdentityService.createTokenQuery().tokenData("test");
        verifyQueryResults(query, 1);

        Token result = query.singleResult();
        assertEquals("test", result.getTokenData());
    }

    public void testQueryByInvalidTokenData() {
        TokenQuery query = idmIdentityService.createTokenQuery().tokenData("invalid");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createTokenQuery().tokenData(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByTokenDataLike() {
        TokenQuery query = idmIdentityService.createTokenQuery().tokenDataLike("%test%");
        verifyQueryResults(query, 1);
    }

    public void testQueryByInvalidTokenDataLike() {
        TokenQuery query = idmIdentityService.createTokenQuery().tokenDataLike("%invalid%");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createTokenQuery().tokenDataLike(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQuerySorting() {
        // asc
        assertEquals(3, idmIdentityService.createTokenQuery().orderByTokenId().asc().count());
        assertEquals(3, idmIdentityService.createTokenQuery().orderByTokenDate().asc().count());

        // desc
        assertEquals(3, idmIdentityService.createTokenQuery().orderByTokenId().desc().count());
        assertEquals(3, idmIdentityService.createTokenQuery().orderByTokenDate().desc().count());

        // Combined with criteria
        TokenQuery query = idmIdentityService.createTokenQuery().userAgentLike("%firefox%").orderByTokenDate().asc();
        List<Token> tokens = query.list();
        assertEquals(2, tokens.size());
        assertEquals("firefox", tokens.get(0).getUserAgent());
        assertEquals("firefox2", tokens.get(1).getUserAgent());
    }

    public void testQueryInvalidSortingUsage() {
        try {
            idmIdentityService.createTokenQuery().orderByTokenId().list();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }

        try {
            idmIdentityService.createTokenQuery().orderByTokenId().orderByTokenDate().list();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    private void verifyQueryResults(TokenQuery query, int countExpected) {
        assertEquals(countExpected, query.list().size());
        assertEquals(countExpected, query.count());

        if (countExpected == 1) {
            assertNotNull(query.singleResult());
        } else if (countExpected > 1) {
            verifySingleResultFails(query);
        } else if (countExpected == 0) {
            assertNull(query.singleResult());
        }
    }

    private void verifySingleResultFails(TokenQuery query) {
        try {
            query.singleResult();
            fail();
        } catch (FlowableException e) {
        }
    }

    public void testNativeQuery() {
        assertEquals("ACT_ID_TOKEN", idmManagementService.getTableName(Token.class));
        assertEquals("ACT_ID_TOKEN", idmManagementService.getTableName(TokenEntity.class));
        String tableName = idmManagementService.getTableName(Token.class);
        String baseQuerySql = "SELECT * FROM " + tableName;

        assertEquals(3, idmIdentityService.createNativeUserQuery().sql(baseQuerySql).list().size());

        assertEquals(1, idmIdentityService.createNativeUserQuery().sql(baseQuerySql + " where ID_ = #{id}").parameter("id", "1111").list().size());

        // paging
        assertEquals(2, idmIdentityService.createNativeUserQuery().sql(baseQuerySql).listPage(0, 2).size());
        assertEquals(2, idmIdentityService.createNativeUserQuery().sql(baseQuerySql).listPage(1, 3).size());
        assertEquals(1, idmIdentityService.createNativeUserQuery().sql(baseQuerySql + " where ID_ = #{id}").parameter("id", "1111").listPage(0, 1).size());
    }

}
