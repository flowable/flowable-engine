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
import org.activiti.app.security.IdentityServiceAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
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

	@Autowired
	protected IdentityServiceAuthenticationProvider authenticationProvider;
	
	@Autowired
	protected Environment env;
	
	//
  // GLOBAL CONFIG
  //
  
  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth) {

    // Default auth (database backed)
    try {
      auth.authenticationProvider(authenticationProvider);
    } catch (Exception e) {
      logger.error("Could not configure authentication mechanism:", e);
    }
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
				.antMatcher("/*-api/**").authorizeRequests()
  			  .antMatchers("/*-api/**").authenticated()
  			.and().httpBasic();
		}
	}
}
