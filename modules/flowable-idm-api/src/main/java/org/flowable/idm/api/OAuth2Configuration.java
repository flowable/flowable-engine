/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.flowable.idm.api;

/**
 *
 * @author ahmedghonim
 */
public interface OAuth2Configuration {

    public String getId();

    public OAuth2AuthorizationHandler getAuthorizationHandler();
    
    public OAuth2AuthorizationRequestor getAuthorizationRequestor();
}
