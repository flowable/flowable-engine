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
package org.activiti.app.conf;

import org.activiti.app.filter.FlowableCookieFilter;
import org.activiti.app.security.AjaxLogoutSuccessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

/**
 * Based on http://docs.spring.io/spring-security/site/docs/3.2.x/reference/htmlsingle/#multiple-httpsecurity
 * 
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
	
	private static final Logger logger = LoggerFactory.getLogger(SecurityConfiguration.class);
	
	public static final String KEY_LDAP_ENABLED = "ldap.authentication.enabled";

  //
	// GLOBAL CONFIG
	//

	@Autowired
	protected Environment env;

	@Bean
	public UserDetailsService userDetailsService() {
		org.activiti.app.security.UserDetailsService userDetailsService = new org.activiti.app.security.UserDetailsService();

		// Undocumented setting to configure the amount of time user data is cached before a new check for validity is made
		// Use <= 0 for always do a check
		userDetailsService.setUserValidityPeriod(env.getProperty("cache.users.recheck.period", Long.class, 30000L));

		return userDetailsService;
	}
	
	//
	// REGULAR WEBAP CONFIG
	//
	
	@Configuration
	@Order(10) // API config first (has Order(1))
  public static class FormLoginWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

	  private static final Logger logger = LoggerFactory.getLogger(FormLoginWebSecurityConfigurerAdapter.class);
	
    @Autowired
    protected Environment env;
    
    @Autowired
    protected FlowableCookieFilter flowableCookieFilter;

    @Autowired
    protected AjaxLogoutSuccessHandler ajaxLogoutSuccessHandler;
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http
        .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
          .addFilterBefore(flowableCookieFilter, UsernamePasswordAuthenticationFilter.class)
          .logout()
              .logoutUrl("/app/logout")
              .logoutSuccessHandler(ajaxLogoutSuccessHandler)
              .deleteCookies("JSESSIONID")
              .and()
          .csrf()
              .disable() // Disabled, cause enabling it will cause sessions
          .headers()
              .frameOptions()
              	.sameOrigin()
              	.addHeaderWriter(new XXssProtectionHeaderWriter());
    }
	}

	//
	// BASIC AUTH
	//

	@Configuration
	@Order(1)
	public static class ApiWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

		protected void configure(HttpSecurity http) throws Exception {

			http
				.sessionManagement()
					.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
					.and()
				.csrf()
					.disable()
				.antMatcher("/api" + "/**")
				.authorizeRequests()
					.antMatchers("/api" + "/**").authenticated()
					.and().httpBasic();
		}
	}

	public static class LdapAuthenticationEnabledCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return context.getEnvironment().getProperty(KEY_LDAP_ENABLED, Boolean.class, false);
		}

	}

}
