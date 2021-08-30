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
package org.flowable.spring.boot.ldap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.flowable.ldap.LDAPConfiguration;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
public class FlowableLdapPropertiesTest {

    @Test
    public void shouldCorrectlyCustomizeLdapConfiguration() {
        FlowableLdapProperties properties = new FlowableLdapProperties();
        properties.setServer("ldap://localhost");
        properties.setPort(33869);
        properties.setUser("filiphr");
        properties.setPassword("demo");
        properties.setInitialContextFactory("com.example.ContextFactory");
        properties.setSecurityAuthentication("test");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key", "value");
        properties.setCustomConnectionParameters(parameters);
        properties.setBaseDn("org.flowable");
        properties.setUserBaseDn("org.flowable.user");
        properties.setGroupBaseDn("org.flowable.group");
        properties.setSearchTimeLimit(1000);
        properties.setConnectionPooling(false);

        FlowableLdapProperties.Query query = properties.getQuery();
        query.setUserById("(&(objectClass=inetOrgPerson)(uid={0}))");
        query.setUserByFullNameLike("(&(objectClass=inetOrgPerson)(|({0}=*{1}*)({2}={3})))");
        query.setAllUsers("(&(objectClass=inetOrgPerson))");
        query.setGroupsForUser("(&(objectClass=groupOfUniqueNames)(uniqueMember={0}))");
        query.setAllGroups("(&(objectClass=groupOfUniqueNames))");
        query.setGroupById("(&(objectClass=groupOfUniqueNames)(uniqueId={0}))");

        FlowableLdapProperties.Attribute attribute = properties.getAttribute();
        attribute.setUserId("id");
        attribute.setFirstName("name");
        attribute.setLastName("lastname");
        attribute.setEmail("mail");
        attribute.setGroupId("groupId");
        attribute.setGroupName("group");
        attribute.setGroupType("gType");

        FlowableLdapProperties.Cache cache = properties.getCache();
        cache.setGroupSize(400);
        cache.setGroupExpiration(5000);

        LDAPConfiguration ldapConfiguration = new LDAPConfiguration();
        properties.customize(ldapConfiguration);

        assertThat(ldapConfiguration)
            .as("Base Ldap Configuration")
            .isEqualToIgnoringGivenFields(properties,
                "queryUserByUserId",
                "queryUserByFullNameLike",
                "queryAllUsers",
                "queryGroupsForUser",
                "queryAllGroups",
                "queryGroupByGroupId",
                "userIdAttribute",
                "userFirstNameAttribute",
                "userLastNameAttribute",
                "userEmailAttribute",
                "groupIdAttribute",
                "groupNameAttribute",
                "groupTypeAttribute",
                "groupCacheSize",
                "groupCacheExpirationTime",
                "ldapQueryBuilder",
                "groupCacheListener"
            );

        assertThat(ldapConfiguration)
            .as("Query properties")
            .extracting("queryUserByUserId",
                "queryUserByFullNameLike",
                "queryAllUsers",
                "queryGroupsForUser",
                "queryAllGroups",
                "queryGroupByGroupId")
            .containsExactly(
                query.getUserById(),
                query.getUserByFullNameLike(),
                query.getAllUsers(),
                query.getGroupsForUser(),
                query.getAllGroups(),
                query.getGroupById()
            );

        assertThat(ldapConfiguration)
            .as("Attribute properties")
            .extracting("userIdAttribute",
                "userFirstNameAttribute",
                "userLastNameAttribute",
                "userEmailAttribute",
                "groupIdAttribute",
                "groupNameAttribute",
                "groupTypeAttribute")
            .containsExactly(
                attribute.getUserId(),
                attribute.getFirstName(),
                attribute.getLastName(),
                attribute.getEmail(),
                attribute.getGroupId(),
                attribute.getGroupName(),
                attribute.getGroupType()
            );

        assertThat(ldapConfiguration)
            .as("Cache properties")
            .extracting("groupCacheSize",
                "groupCacheExpirationTime")
            .containsExactly(
                cache.getGroupSize(),
                cache.getGroupExpiration()
            );
    }
}