package org.activiti.app.security;

import java.util.ArrayList;
import java.util.List;

import org.activiti.idm.api.IdmIdentityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class IdentityServiceAuthenticationProvider implements AuthenticationProvider {
  
  @Autowired
  protected IdmIdentityService identityService;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    boolean authenticated = identityService.checkPassword((String) authentication.getPrincipal(), 
        (String) authentication.getCredentials());
    
    if (authenticated) {
      List<GrantedAuthority> grantedAuths = new ArrayList<>();
      grantedAuths.add(new SimpleGrantedAuthority(AuthoritiesConstants.USER));
      Authentication auth = new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), 
          authentication.getCredentials(), grantedAuths);
      return auth;
      
    } else {
      return null;
    }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return true;
  }

}
