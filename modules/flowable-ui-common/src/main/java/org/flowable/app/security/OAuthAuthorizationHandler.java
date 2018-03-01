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
public interface OAuthAuthorizationHandler {

    public boolean canHandle(String state);

    public FlowableAppUser handle(String state, String code);

    public String successRedirectUrl(String state);
    
}
