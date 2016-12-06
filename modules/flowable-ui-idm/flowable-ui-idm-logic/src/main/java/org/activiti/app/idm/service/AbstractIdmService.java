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
package org.activiti.app.idm.service;

import java.util.Collection;

import org.activiti.app.security.DefaultPrivileges;
import org.activiti.app.security.SecurityUtils;
import org.activiti.app.service.exception.NotPermittedException;
import org.flowable.idm.api.IdmIdentityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author Joram Barrez
 */
public class AbstractIdmService {
  
  @Autowired
  protected IdmIdentityService identityService;
  
}
