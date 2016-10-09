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
package org.activiti.app.security;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is called AFTER successful authentication, to populate the user object with additional details The default (no ldap) way of authentication is a bit hidden in Spring Security magic. But
 * basically, the user object is fetched from the db and the hashed password is compared with the hash of the provided password (using the Spring {@link StandardPasswordEncoder}).
 */
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService, CustomUserDetailService {

  @Autowired
  private Environment env;

  private long userValidityPeriod;

  @Override
  @Transactional
  public UserDetails loadUserByUsername(final String login) {

    // This method is only called during the login.
    // All subsequent calls use the method with the long userId as parameter.
    // (Hence why the cache is NOT used here, but it is used in the loadByUserId)

    String actualLogin = login;
    
    return new User("admin", "admin", new ArrayList());
  }

  @Transactional
  public UserDetails loadByUserId(final String userId) {
    return new User("admin", "admin", new ArrayList());
  }

  public void setUserValidityPeriod(long userValidityPeriod) {
    this.userValidityPeriod = userValidityPeriod;
  }
}
