/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.flowable.app.security;

import java.util.Collection;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 *
 * @author ahmedghonim
 */
public class OAuthAuthenticationImpl extends UsernamePasswordAuthenticationToken implements OAuthAuthentication {

    private String tokenData;

    @Override
    public String getTokenData() {
        return tokenData;
    }

    public void setTokenData(String tokenData) {
        this.tokenData = tokenData;
    }

    public OAuthAuthenticationImpl(String tokenData, Object principal, Object credentials) {
        super(principal, credentials);
        this.tokenData = tokenData;
    }

    public OAuthAuthenticationImpl(String tokenData, Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
        this.tokenData = tokenData;
    }

}
