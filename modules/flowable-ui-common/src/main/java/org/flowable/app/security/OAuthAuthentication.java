/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.flowable.app.security;

import org.springframework.security.core.Authentication;

/**
 *
 * @author ahmedghonim
 */
public interface OAuthAuthentication extends Authentication {

    public String getTokenData();
}
