/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.flowable.app.security;

/**
 *
 * @author ahmedghonim
 */
public interface OAuth2AuthorizationRequestor {

    public String getName();
    
    public boolean canAuthorize(String id);

    public String buildAuthorizationURL();

}
