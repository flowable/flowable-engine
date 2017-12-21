package org.flowable.rest.conf;

import org.flowable.rest.security.BasicAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    protected Environment environment;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        return new BasicAuthenticationProvider();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        HttpSecurity httpSecurity = http.authenticationProvider(authenticationProvider())
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .csrf().disable();

        boolean swaggerDocsEnable = environment.getProperty("rest.docs.swagger.enabled", Boolean.class, true);
        if (swaggerDocsEnable) {
            httpSecurity
                    .authorizeRequests()
                    .antMatchers("/docs/**").permitAll()
                    .anyRequest()
                    .authenticated().and().httpBasic();
        } else {
            httpSecurity
                    .authorizeRequests()
                    .antMatchers("/docs/**").denyAll()
                    .anyRequest()
                    .authenticated().and().httpBasic();
        }
    }
}
