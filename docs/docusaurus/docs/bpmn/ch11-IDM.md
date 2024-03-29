---
id: ch11-IDM
title: Identity management
---

Starting from Flowable V6, the identity management (IDM) component has been extracted from the flowable-engine module and the logic moved to several separate modules: flowable-idm-api, flowable-idm-engine, flowable-idm-spring and flowable-idm-engine-configurator. The main reason for separating the IDM logic was that it’s not core to the Flowable engine and in a lot of cases when the Flowable engine is embedded in an application, the identity logic is not used or needed.

By default, the IDM engine is initialized and started when the Flowable engine is started. This results in the same identity logic being executed and available in Flowable v5. The idm-engine manages its own database schema and the following entities:

-   User and UserEntity, the user information.

-   Group and GroupEntity, the group information.

-   MembershipEntity, the memberships of users in groups

-   Privilege and PrivilegeEntity, a privilege definition (for example used for controlling access to the UI apps, such as the Flowable Modeler and Flowable Task app)

-   PrivilegeMappingEntity, linking a user and/or group to a privilege

-   Token and TokenEntity, an authentication token used by the UI apps

Since the DB contains historic entities for past as well as ongoing instances, you might want to consider querying these tables in order to minimize access to the runtime process instance data, and that way keep the runtime execution performant.

## IDM engine configuration

By default the Flowable engine is started with the org.flowable.idm.engine.configurator.IdmEngineConfigurator. This configurator uses the same datasource configuration as the Flowable process engine configuration. No additional configuration is needed to use the identity component as it was configured in Flowable v5.

When no identity logic is needed in the Flowable engine the IDM engine can be disabled in the process engine configuration.

    <bean id="processEngineConfiguration" class="org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration">
      <property name="disableIdmEngine" value="true" />
      ...
    </bean>

This means that no user and group queries can be used, and candidate groups in a task query can not be retrieved for a user.

By default, the user passwords will be saved in plain text in the IDM database tables. To make sure that the passwords are encoded you can define a password encoder in the process engine configuration.

    <bean id="bCryptEncoder"
          class="org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder"/>

    <bean id="passwordEncoder" class="org.flowable.idm.spring.authentication.SpringEncoder">
        <constructor-arg ref="bCryptEncoder"/>
    </bean>

    <bean id="processEngineConfiguration" class="org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration">
      <property name="passwordEncoder" ref="passwordEncoder" />
      ...
    </bean>

In this example the ShaPasswordEncoder is used, but you can also use the org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder for example. When not using Spring you can also use the org.flowable.idm.engine.impl.authentication.ApacheDigester to encode the passwords.

The default IDM engine configurator can also be overridden to initialize the IDM Engine in a custom way. A good example is the LDAPConfigurator
implementation which overrides the default IDM engine to use a LDAP server instead of the default IDM database tables. The idmProcessEngineConfigurator property of the process engine configuration can be used to set a custom configurator like the LDAPConfigurator

    <bean id="processEngineConfiguration" class="...SomeProcessEngineConfigurationClass">
        ...
        <property name="idmProcessEngineConfigurator">
          <bean class="org.flowable.ldap.LDAPConfigurator">

            <!-- Server connection params -->
            <property name="server" value="ldap://localhost" />
            <property name="port" value="33389" />
            <property name="user" value="uid=admin, ou=users, o=flowable" />
            <property name="password" value="pass" />

            <!-- Query params -->
            <property name="baseDn" value="o=flowable" />
            <property name="queryUserByUserId" value="(&(objectClass=inetOrgPerson)(uid={0}))" />
            <property name="queryUserByFullNameLike" value="(&(objectClass=inetOrgPerson)(|({0}=*{1}*)({2}=*{3}*)))" />
            <property name="queryGroupsForUser" value="(&(objectClass=groupOfUniqueNames)(uniqueMember={0}))" />

            <!-- Attribute config -->
            <property name="userIdAttribute" value="uid" />
            <property name="userFirstNameAttribute" value="cn" />
            <property name="userLastNameAttribute" value="sn" />
            <property name="userEmailAttribute" value="mail" />


            <property name="groupIdAttribute" value="cn" />
            <property name="groupNameAttribute" value="cn" />

          </bean>
        </property>
    </bean>
