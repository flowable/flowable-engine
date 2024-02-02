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
package flowable;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Privilege;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;

/**
 * @author Filip Hrisafov
 */
@SpringBootApplication(proxyBeanMethods = false)
public class SampleLdapApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleLdapApplication.class, args);
    }

    @Configuration(proxyBeanMethods = false)
    class ApiWebSecurityConfigurationAdapter {

        @Bean
        @Order(99)
        public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .securityMatcher(antMatcher("/process-api/**"))
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers(antMatcher("/process-api/repository/**")).hasAnyAuthority("repository-privilege")
                        .requestMatchers(antMatcher("/process-api/management/**")).hasAnyAuthority("management-privilege")
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

            return http.build();
        }
    }

    @Bean
    @ConditionalOnBean(InMemoryDirectoryServer.class)
    public CommandLineRunner initializePrivileges(IdmIdentityService idmIdentityService) {
        return args -> {
            Privilege repositoryPrivilege = idmIdentityService.createPrivilege("repository-privilege");
            Privilege managementPrivilege = idmIdentityService.createPrivilege("management-privilege");

            idmIdentityService.addGroupPrivilegeMapping(repositoryPrivilege.getId(), "user");
            idmIdentityService.addGroupPrivilegeMapping(managementPrivilege.getId(), "admin");
            idmIdentityService.addUserPrivilegeMapping(managementPrivilege.getId(), "fozzie");
        };
    }
}
