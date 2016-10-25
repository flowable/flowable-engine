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
package org.activiti.conf;

import org.activiti.filter.FlowableCookieFilter;
import org.activiti.security.AjaxLogoutSuccessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(SecurityConfiguration.class);

  //
  // FORM LOGIN (Cookie based)
  //

  @Configuration
  public static class FormLoginWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

    @Autowired
    private Environment env;

    @Autowired
    protected FlowableCookieFilter flowableCookieFilter;

    @Autowired
    private AjaxLogoutSuccessHandler ajaxLogoutSuccessHandler;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http
        .addFilterBefore(flowableCookieFilter, UsernamePasswordAuthenticationFilter.class)
        .logout()
            .logoutUrl("/app/logout")
            .logoutSuccessHandler(ajaxLogoutSuccessHandler)
            .deleteCookies("JSESSIONID")
            .and()
        .csrf()
            .disable();
    }
  }
}
