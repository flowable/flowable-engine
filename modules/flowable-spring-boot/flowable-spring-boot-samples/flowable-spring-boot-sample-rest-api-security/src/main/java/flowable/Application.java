package flowable;

import org.flowable.idm.api.Group;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.User;
import org.flowable.rest.security.BasicAuthenticationProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Application {

    @Order(99)
    @Configuration
    static class ApiWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .antMatcher("/api/**")
                    .authorizeRequests()
                    .anyRequest().authenticated()
                    .and()
                    .httpBasic();
        }
    }

    @Bean
    CommandLineRunner seedUsersAndGroups(final IdmIdentityService identityService) {
        return new CommandLineRunner() {
            @Override
            public void run(String... strings) throws Exception {

                // install groups & users
                Group adminGroup = identityService.newGroup("admin");
                adminGroup.setName("admin");
                adminGroup.setType("security-role");
                identityService.saveGroup(adminGroup);

                Group group = identityService.newGroup("user");
                group.setName("users");
                group.setType("security-role");
                identityService.saveGroup(group);

                Privilege userPrivilege = identityService.createPrivilege("user-privilege");
                identityService.addGroupPrivilegeMapping(userPrivilege.getId(), group.getId());

                Privilege adminPrivilege = identityService.createPrivilege("admin-privilege");

                User joram = identityService.newUser("jbarrez");
                joram.setFirstName("Joram");
                joram.setLastName("Barrez");
                joram.setPassword("password");
                identityService.saveUser(joram);
                identityService.addUserPrivilegeMapping(adminPrivilege.getId(), joram.getId());

                User filip = identityService.newUser("filiphr");
                filip.setFirstName("Filip");
                filip.setLastName("Hrisafov");
                filip.setPassword("password");
                identityService.saveUser(filip);

                User josh = identityService.newUser("jlong");
                josh.setFirstName("Josh");
                josh.setLastName("Long");
                josh.setPassword("password");
                identityService.saveUser(josh);

                identityService.createMembership("jbarrez", "user");
                identityService.createMembership("jbarrez", "admin");
                identityService.createMembership("filiphr", "user");
                identityService.createMembership("jlong", "user");
            }
        };
    }

    public static void main(String args[]) {
        SpringApplication.run(Application.class, args);
    }

    @Configuration
    @EnableWebSecurity
    public static class SecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Bean
        public AuthenticationProvider authenticationProvider() {
            return new BasicAuthenticationProvider();
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                .authenticationProvider(authenticationProvider())
                .csrf().disable()
                .authorizeRequests()
                .anyRequest().authenticated()
                .and()
                .httpBasic();
        }
    }
}