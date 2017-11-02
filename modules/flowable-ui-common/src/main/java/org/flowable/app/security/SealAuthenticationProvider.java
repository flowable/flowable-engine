/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.flowable.app.security;

import java.util.Collection;
import java.util.HashSet;
import org.flowable.idm.api.User;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;

/**
 *
 * @author ahmedghonim
 */
public class SealAuthenticationProvider implements AuthenticationProvider {

    @Override
    public Authentication authenticate(final Authentication a) throws AuthenticationException {
        User u = new User() {
            @Override
            public String getId() {
                return a.getPrincipal().toString();
            }

            @Override
            public void setId(String id) {

            }

            @Override
            public String getFirstName() {
                return a.getPrincipal().toString();
            }

            @Override
            public void setFirstName(String firstName) {

            }

            @Override
            public void setLastName(String lastName) {

            }

            @Override
            public String getLastName() {
                return a.getPrincipal().toString();
            }

            @Override
            public void setEmail(String email) {

            }

            @Override
            public String getEmail() {
                return a.getPrincipal().toString();
            }

            @Override
            public String getPassword() {
                return a.getCredentials().toString();
            }

            @Override
            public void setPassword(String string) {

            }

            @Override
            public boolean isPictureSet() {
                return false;
            }
        };
        Collection<GrantedAuthority> auth = new HashSet<>();
        FlowableAppUser user = new FlowableAppUser(u, a.getPrincipal().toString(), auth);
        return new UsernamePasswordAuthenticationToken(user, a.getCredentials().toString(), auth);
    }

    @Override
    public boolean supports(Class<?> type) {
        return true;
    }

}
