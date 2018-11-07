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
package org.flowable.spring.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.flowable.idm.api.Group;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.User;
import org.flowable.idm.engine.test.PluggableFlowableIdmTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * @author Filip Hrisafov
 */
public class FlowableUserDetailsServiceTest extends PluggableFlowableIdmTestCase {

    private UserDetailsService userDetailsService;

    @BeforeEach
    protected void setUp() throws Exception {
        createGroup("admins", "Admins", "user");
        createGroup("sales", "Sales", "user");
        createGroup("engineering", "Engineering", "tech");

        createUser("kermit", "Kermit", "the Frog", "Kermit the Frog", "kermit@muppetshow.com");
        createUser("fozzie", "Fozzie", "Bear", "Fozzie Bear", "fozzie@muppetshow.com");

        idmIdentityService.createMembership("kermit", "admins");
        idmIdentityService.createMembership("kermit", "sales");
        idmIdentityService.createMembership("kermit", "engineering");
        idmIdentityService.createMembership("fozzie", "sales");

        String adminPrivilegename = "access admin application";
        Privilege adminPrivilege = idmIdentityService.createPrivilege(adminPrivilegename);
        idmIdentityService.addGroupPrivilegeMapping(adminPrivilege.getId(), "admins");

        String modelerPrivilegeName = "access modeler application";
        Privilege modelerPrivilege = idmIdentityService.createPrivilege(modelerPrivilegeName);
        idmIdentityService.addGroupPrivilegeMapping(modelerPrivilege.getId(), "admins");
        idmIdentityService.addGroupPrivilegeMapping(modelerPrivilege.getId(), "engineering");
        idmIdentityService.addUserPrivilegeMapping(modelerPrivilege.getId(), "kermit");

        String startProcessesPrivilegename = "start processes";
        Privilege startProcessesPrivilege = idmIdentityService.createPrivilege(startProcessesPrivilegename);
        idmIdentityService.addGroupPrivilegeMapping(startProcessesPrivilege.getId(), "sales");

        userDetailsService = new FlowableUserDetailsService(idmIdentityService);
    }

    private User createUser(String id, String firstName, String lastName, String displayName, String email) {
        User user = idmIdentityService.newUser(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setPassword(id);
        idmIdentityService.saveUser(user);
        return user;
    }

    @AfterEach
    protected void tearDown() throws Exception {
        clearAllUsersAndGroups();
    }

    @Test
    public void testLoadingByUnknownUserShouldThrowException() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("user (unknown) could not be found");
    }

    @Test
    public void testLoadingByNullUserShouldIgnoreFlowableException() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("user (null) could not be found");
    }

    @Test
    public void testLoadingKnownUserWithAllPrivileges() {
        UserDetails kermit = userDetailsService.loadUserByUsername("kermit");

        assertThat(kermit).isNotNull();
        assertThat(kermit.isCredentialsNonExpired()).as("credentialsNonExpired").isTrue();
        assertThat(kermit.isAccountNonLocked()).as("accountNonLocked").isTrue();
        assertThat(kermit.isAccountNonExpired()).as("accountNonExpired").isTrue();
        assertThat(kermit.isEnabled()).as("enabled").isTrue();
        assertThat(kermit.getUsername()).as("username").isEqualTo("kermit");
        assertThat(kermit.getPassword()).as("password").isEqualTo("kermit");
        assertThat(kermit.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .as("granted authorities")
            .containsExactly(
                "access admin application",
                "access modeler application",
                "start processes"
            );

        assertThat(kermit).isInstanceOf(FlowableUserDetails.class);

        FlowableUserDetails kermitFlowable = (FlowableUserDetails) kermit;

        User user = kermitFlowable.getUser();
        assertThat(user.getId()).isEqualTo("kermit");
        assertThat(user.getFirstName()).isEqualTo("Kermit");
        assertThat(user.getLastName()).isEqualTo("the Frog");
        assertThat(user.getDisplayName()).isEqualTo("Kermit the Frog");
        assertThat(user.getEmail()).isEqualTo("kermit@muppetshow.com");
        assertThat(user.getPassword()).isEqualTo("kermit");

        user.setId("test");
        user.setFirstName("test");
        user.setLastName("test");
        user.setDisplayName("test");
        user.setEmail("test");

        assertThat(user.getId()).isEqualTo("kermit");
        assertThat(user.getFirstName()).isEqualTo("Kermit");
        assertThat(user.getLastName()).isEqualTo("the Frog");
        assertThat(user.getDisplayName()).isEqualTo("Kermit the Frog");
        assertThat(user.getEmail()).isEqualTo("kermit@muppetshow.com");

        assertThat(kermitFlowable.getGroups())
            .extracting(Group::getId, Group::getName, Group::getType)
            .as("Groups")
            .containsExactlyInAnyOrder(
                tuple("admins", "Admins", "user"),
                tuple("sales", "Sales", "user"),
                tuple("engineering", "Engineering", "tech")
            );

        kermitFlowable.getGroups().forEach(group -> {
            group.setId("test");
            group.setType("test");
            group.setName("test");
        });

        assertThat(kermitFlowable.getGroups())
            .extracting(Group::getId, Group::getName, Group::getType)
            .as("Groups")
            .containsExactlyInAnyOrder(
                tuple("admins", "Admins", "user"),
                tuple("sales", "Sales", "user"),
                tuple("engineering", "Engineering", "tech")
            );

        assertThat(kermit).isInstanceOf(CredentialsContainer.class);
        CredentialsContainer container = (CredentialsContainer) kermit;
        container.eraseCredentials();
        assertThat(kermit.getPassword()).as("Password after erase").isNull();
        assertThat(kermitFlowable.getUser().getPassword()).as("User password after erase").isNull();
    }

    @Test
    public void testLoadingUserShouldBeCaseSensitive() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("kErMiT"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("user (kErMiT) could not be found");
    }

    @Test
    public void testLoadingKnownUserWithSomePrivileges() {
        UserDetails fozzie = userDetailsService.loadUserByUsername("fozzie");

        assertThat(fozzie).isNotNull();
        assertThat(fozzie.isCredentialsNonExpired()).as("credentialsNonExpired").isTrue();
        assertThat(fozzie.isAccountNonLocked()).as("accountNonLocked").isTrue();
        assertThat(fozzie.isAccountNonExpired()).as("accountNonExpired").isTrue();
        assertThat(fozzie.isEnabled()).as("enabled").isTrue();
        assertThat(fozzie.getUsername()).as("username").isEqualTo("fozzie");
        assertThat(fozzie.getPassword()).as("password").isEqualTo("fozzie");
        assertThat(fozzie.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .as("granted authorities")
            .containsExactly(
                "start processes"
            );

        assertThat(fozzie).isInstanceOf(FlowableUserDetails.class);

        FlowableUserDetails fozzieFlowable = (FlowableUserDetails) fozzie;

        User user = fozzieFlowable.getUser();
        assertThat(user.getId()).isEqualTo("fozzie");
        assertThat(user.getFirstName()).isEqualTo("Fozzie");
        assertThat(user.getLastName()).isEqualTo("Bear");
        assertThat(user.getDisplayName()).isEqualTo("Fozzie Bear");
        assertThat(user.getEmail()).isEqualTo("fozzie@muppetshow.com");
        assertThat(user.getPassword()).isEqualTo("fozzie");

        user.setId("test");
        user.setFirstName("test");
        user.setLastName("test");
        user.setDisplayName("test");
        user.setEmail("test");

        assertThat(user.getId()).isEqualTo("fozzie");
        assertThat(user.getFirstName()).isEqualTo("Fozzie");
        assertThat(user.getLastName()).isEqualTo("Bear");
        assertThat(user.getDisplayName()).isEqualTo("Fozzie Bear");
        assertThat(user.getEmail()).isEqualTo("fozzie@muppetshow.com");

        assertThat(fozzieFlowable.getGroups())
            .extracting(Group::getId, Group::getName, Group::getType)
            .as("Groups")
            .containsExactlyInAnyOrder(
                tuple("sales", "Sales", "user")
            );
    }

    @Test
    public void testSerializingUserDetailsShouldWorkCorrectly() throws IOException, ClassNotFoundException {
        UserDetails kermit = userDetailsService.loadUserByUsername("kermit");

        byte[] serialized;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(buffer);
        outputStream.writeObject(kermit);
        outputStream.close();
        serialized = buffer.toByteArray();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(serialized);
        ObjectInputStream stream = new ObjectInputStream(inputStream);
        Object deserialized = stream.readObject();
        stream.close();

        assertThat(deserialized).isInstanceOf(FlowableUserDetails.class);

        kermit = (UserDetails) deserialized;
        assertThat(kermit.isCredentialsNonExpired()).as("credentialsNonExpired").isTrue();
        assertThat(kermit.isAccountNonLocked()).as("accountNonLocked").isTrue();
        assertThat(kermit.isAccountNonExpired()).as("accountNonExpired").isTrue();
        assertThat(kermit.isEnabled()).as("enabled").isTrue();
        assertThat(kermit.getUsername()).as("username").isEqualTo("kermit");
        assertThat(kermit.getPassword()).as("password").isEqualTo("kermit");
        assertThat(kermit.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .as("granted authorities")
            .containsExactly(
                "access admin application",
                "access modeler application",
                "start processes"
            );

        FlowableUserDetails kermitFlowable = (FlowableUserDetails) kermit;

        User user = kermitFlowable.getUser();
        assertThat(user.getId()).isEqualTo("kermit");
        assertThat(user.getFirstName()).isEqualTo("Kermit");
        assertThat(user.getLastName()).isEqualTo("the Frog");
        assertThat(user.getDisplayName()).isEqualTo("Kermit the Frog");
        assertThat(user.getEmail()).isEqualTo("kermit@muppetshow.com");
        assertThat(user.getPassword()).isEqualTo("kermit");

        assertThat(kermitFlowable.getGroups())
            .extracting(Group::getId, Group::getName, Group::getType)
            .as("Groups")
            .containsExactlyInAnyOrder(
                tuple("admins", "Admins", "user"),
                tuple("sales", "Sales", "user"),
                tuple("engineering", "Engineering", "tech")
            );
    }
}
