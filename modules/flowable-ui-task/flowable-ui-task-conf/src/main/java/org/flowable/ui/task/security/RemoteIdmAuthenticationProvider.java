package org.flowable.ui.task.security;

import java.util.ArrayList;
import java.util.Collection;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.ui.common.model.RemoteUser;
import org.flowable.ui.common.service.idm.RemoteIdmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class RemoteIdmAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    protected RemoteIdmService remoteIdmService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        RemoteUser user = remoteIdmService.authenticateUser(authentication.getPrincipal().toString(), authentication.getCredentials().toString());
        if (user == null) {
            throw new FlowableException("user not found " + authentication.getPrincipal());
        }

        Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        for (String privilege : user.getPrivileges()) {
            grantedAuthorities.add(new SimpleGrantedAuthority(privilege));
        }

        org.flowable.common.engine.impl.identity.Authentication.setAuthenticatedUserId(user.getId());

        Authentication auth = new UsernamePasswordAuthenticationToken(authentication.getPrincipal(),
                authentication.getCredentials(), grantedAuthorities);
        return auth;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return true;
    }
}
