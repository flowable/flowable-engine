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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class TokenQueryTest extends PluggableFlowableIdmTestCase {

    @BeforeEach
    protected void setUp() throws Exception {
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

    @AfterEach
    protected void tearDown() throws Exception {
        idmIdentityService.deleteToken("1111");
        idmIdentityService.deleteToken("2222");
        idmIdentityService.deleteToken("3333");
    }

    @Test
    public void testQueryByNoCriteria() {
        TokenQuery query = idmIdentityService.createTokenQuery();
        verifyQueryResults(query, 3);
    }

    @Test
    public void testQueryById() {
        TokenQuery query = idmIdentityService.createTokenQuery().tokenId("1111");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidId() {
        TokenQuery query = idmIdentityService.createTokenQuery().tokenId("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createTokenQuery().tokenId(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByIds() {
        List<String> ids = new ArrayList<>();
        ids.add("1111");
        TokenQuery query = idmIdentityService.createTokenQuery().tokenIds(ids);
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidIds() {
        List<String> ids = new ArrayList<>();
        ids.add("invalid");
        TokenQuery query = idmIdentityService.createTokenQuery().tokenIds(ids);
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createTokenQuery().tokenIds(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByTokenValue() {
        TokenQuery query = idmIdentityService.createTokenQuery().tokenValue("aaaa");
        verifyQueryResults(query, 1);

        Token result = query.singleResult();
        assertThat(result.getTokenValue()).isEqualTo("aaaa");
    }

    @Test
    public void testQueryByInvalidTokenValue() {
        TokenQuery query = idmIdentityService.createTokenQuery().tokenValue("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createTokenQuery().tokenValue(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByTokenDate() {
        Calendar queryCal = new GregorianCalendar(2099, 1, 1, 0, 0, 0);
        TokenQuery query = idmIdentityService.createTokenQuery().tokenDate(queryCal.getTime());
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createTokenQuery().tokenDate(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByTokenDateBefore() {
        Calendar queryCal = new GregorianCalendar(2015, 6, 1, 0, 0, 0);
        TokenQuery query = idmIdentityService.createTokenQuery().tokenDateBefore(queryCal.getTime());
        verifyQueryResults(query, 1);

        Token result = query.singleResult();
        Calendar tokenCal = new GregorianCalendar();
        tokenCal.setTime(result.getTokenDate());
        assertThat(tokenCal.get(Calendar.YEAR)).isEqualTo(2015);
        assertThat(tokenCal.get(Calendar.MONTH)).isEqualTo(1);

        assertThatThrownBy(() -> idmIdentityService.createTokenQuery().tokenDateBefore(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByTokenDateAfter() {
        Calendar queryCal = new GregorianCalendar(2016, 6, 1, 0, 0, 0);
        TokenQuery query = idmIdentityService.createTokenQuery().tokenDateAfter(queryCal.getTime());
        verifyQueryResults(query, 1);

        Token result = query.singleResult();
        Calendar tokenCal = new GregorianCalendar();
        tokenCal.setTime(result.getTokenDate());
        assertThat(tokenCal.get(Calendar.YEAR)).isEqualTo(2017);
        assertThat(tokenCal.get(Calendar.MONTH)).isEqualTo(1);

        assertThatThrownBy(() -> idmIdentityService.createTokenQuery().tokenDateAfter(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByIpAddress() {
        TokenQuery query = idmIdentityService.createTokenQuery().ipAddress("127.0.0.1");
        verifyQueryResults(query, 1);

        Token result = query.singleResult();
        assertThat(result.getIpAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    public void testQueryByInvalidIpAddress() {
        TokenQuery query = idmIdentityService.createTokenQuery().ipAddress("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createTokenQuery().ipAddress(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByIpAddressLike() {
        TokenQuery query = idmIdentityService.createTokenQuery().ipAddressLike("%0.0.1%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createTokenQuery().ipAddressLike("129.%");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidIpAddressLike() {
        TokenQuery query = idmIdentityService.createTokenQuery().ipAddressLike("%invalid%");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createTokenQuery().ipAddressLike(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByUserAgent() {
        TokenQuery query = idmIdentityService.createTokenQuery().userAgent("chrome");
        verifyQueryResults(query, 1);

        Token result = query.singleResult();
        assertThat(result.getUserAgent()).isEqualTo("chrome");
    }

    @Test
    public void testQueryByInvalidUserAgent() {
        TokenQuery query = idmIdentityService.createTokenQuery().userAgent("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createTokenQuery().userAgent(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByUserAgentLike() {
        TokenQuery query = idmIdentityService.createTokenQuery().userAgentLike("%fire%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createTokenQuery().userAgentLike("ch%");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidUserAgentLike() {
        TokenQuery query = idmIdentityService.createTokenQuery().userAgentLike("%invalid%");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createTokenQuery().userAgentLike(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

    }

    @Test
    public void testQueryByUserId() {
        TokenQuery query = idmIdentityService.createTokenQuery().userId("user1");
        verifyQueryResults(query, 1);

        Token result = query.singleResult();
        assertThat(result.getUserId()).isEqualTo("user1");
    }

    @Test
    public void testQueryByInvalidUserId() {
        TokenQuery query = idmIdentityService.createTokenQuery().userId("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createTokenQuery().userId(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

    }

    @Test
    public void testQueryByUserIdLike() {
        TokenQuery query = idmIdentityService.createTokenQuery().userIdLike("%user%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createTokenQuery().userIdLike("bla%");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidUserIdLike() {
        TokenQuery query = idmIdentityService.createTokenQuery().userIdLike("%invalid%");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createTokenQuery().userIdLike(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByTokenData() {
        TokenQuery query = idmIdentityService.createTokenQuery().tokenData("test");
        verifyQueryResults(query, 1);

        Token result = query.singleResult();
        assertThat(result.getTokenData()).isEqualTo("test");
    }

    @Test
    public void testQueryByInvalidTokenData() {
        TokenQuery query = idmIdentityService.createTokenQuery().tokenData("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createTokenQuery().tokenData(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByTokenDataLike() {
        TokenQuery query = idmIdentityService.createTokenQuery().tokenDataLike("%test%");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidTokenDataLike() {
        TokenQuery query = idmIdentityService.createTokenQuery().tokenDataLike("%invalid%");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createTokenQuery().tokenDataLike(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQuerySorting() {
        // asc
        assertThat(idmIdentityService.createTokenQuery().orderByTokenId().asc().count()).isEqualTo(3);
        assertThat(idmIdentityService.createTokenQuery().orderByTokenDate().asc().count()).isEqualTo(3);

        // desc
        assertThat(idmIdentityService.createTokenQuery().orderByTokenId().desc().count()).isEqualTo(3);
        assertThat(idmIdentityService.createTokenQuery().orderByTokenDate().desc().count()).isEqualTo(3);

        // Combined with criteria
        TokenQuery query = idmIdentityService.createTokenQuery().userAgentLike("%firefox%").orderByTokenDate().asc();
        List<Token> tokens = query.list();
        assertThat(tokens)
                .extracting(Token::getUserAgent)
                .containsExactly("firefox", "firefox2");
    }

    @Test
    public void testQueryInvalidSortingUsage() {
        assertThatThrownBy(() -> idmIdentityService.createTokenQuery().orderByTokenId().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> idmIdentityService.createTokenQuery().orderByTokenId().orderByTokenDate().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    private void verifyQueryResults(TokenQuery query, int countExpected) {
        assertThat(query.list()).hasSize(countExpected);
        assertThat(query.count()).isEqualTo(countExpected);

        if (countExpected == 1) {
            assertThat(query.singleResult()).isNotNull();
        } else if (countExpected > 1) {
            verifySingleResultFails(query);
        } else if (countExpected == 0) {
            assertThat(query.singleResult()).isNull();
        }
    }

    private void verifySingleResultFails(TokenQuery query) {
        assertThatThrownBy(() -> query.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testNativeQuery() {
        assertThat(idmManagementService.getTableName(Token.class)).isEqualTo("ACT_ID_TOKEN");
        assertThat(idmManagementService.getTableName(TokenEntity.class)).isEqualTo("ACT_ID_TOKEN");
        String tableName = idmManagementService.getTableName(Token.class);
        String baseQuerySql = "SELECT * FROM " + tableName;

        assertThat(idmIdentityService.createNativeUserQuery().sql(baseQuerySql).list()).hasSize(3);

        assertThat(idmIdentityService.createNativeUserQuery().sql(baseQuerySql + " where ID_ = #{id}").parameter("id", "1111").list()).hasSize(1);

        // paging
        assertThat(idmIdentityService.createNativeUserQuery().sql(baseQuerySql).listPage(0, 2)).hasSize(2);
        assertThat(idmIdentityService.createNativeUserQuery().sql(baseQuerySql).listPage(1, 3)).hasSize(2);
        assertThat(idmIdentityService.createNativeUserQuery().sql(baseQuerySql + " where ID_ = #{id}").parameter("id", "1111").listPage(0, 1)).hasSize(1);
    }

}
