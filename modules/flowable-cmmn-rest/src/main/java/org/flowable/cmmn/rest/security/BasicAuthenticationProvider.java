package org.flowable.cmmn.rest.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flowable.idm.api.Group;
import org.flowable.idm.api.IdmIdentityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class BasicAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    @Lazy
    private IdmIdentityService identityService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String name = authentication.getName();
        String password = authentication.getCredentials().toString();

        boolean authenticated = identityService.checkPassword(name, password);
        if (authenticated) {
            List<Group> groups = identityService.createGroupQuery().groupMember(name).list();
            Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
            for (Group group : groups) {
                grantedAuthorities.add(new SimpleGrantedAuthority(group.getId()));
            }
            return new UsernamePasswordAuthenticationToken(name, password, grantedAuthorities);
        } else {
            throw new BadCredentialsException("Authentication failed for this username and password");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
