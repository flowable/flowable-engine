<#-- This is based on https://github.com/mojohaus/license-maven-plugin/blob/master/src/main/resources/org/codehaus/mojo/license/third-party-file-groupByMultiLicense.ftl -->
<#-- Format artifact "name (groupId:artifactId:version - url)" -->
<#function artifactFormat artifact>
    <#if artifact.name?index_of('Unnamed') &gt; -1>
        <#return artifact.artifactId + " (" + artifact.groupId + ":" + artifact.artifactId + ":" + artifact.version + " - " + (artifact.url!"no url defined") + ")">
    <#else>
        <#return artifact.name + " (" + artifact.groupId + ":" + artifact.artifactId + ":" + artifact.version + " - " + (artifact.url!"no url defined") + ")">
    </#if>
</#function>

<#-- Create a key from provided licenses list, ordered alphabetically: "license A, license B, license C" -->
<#function licensesKey licenses>
    <#local result = "">
    <#list licenses?sort as license>
        <#local result=result + ", " + license>
    </#list>
    <#return result?substring(2)>
</#function>

<#-- Aggregate dependencies map for generated license key (support for multi-license) and convert artifact to string -->
<#function aggregateLicenses dependencies>
    <#assign aggregate = {}>
    <#assign flowableAggregate = []>
    <#list dependencies as entry>
        <#assign artifact = entry.getKey()/>
        <#if artifact.groupId == 'org.flowable'>
            <#assign flowableAggregate = flowableAggregate + [artifact.artifactId + ":" + artifact.version] />
        <#else>
            <#assign project = artifactFormat(artifact)/>
            <#assign licenses = entry.getValue()/>
            <#assign key = licensesKey(licenses)/>
            <#if aggregate[key]?? >
                <#assign replacement = aggregate[key] + [project] />
                <#assign aggregate = aggregate + {key:replacement} />
            <#else>
                <#assign aggregate = aggregate + {key:[project]} />
            </#if>
        </#if>

    </#list>
    <#return {"aggregate": aggregate, "flowable": flowableAggregate}>
</#function>
Flowable (http://www.flowable.org) includes work under the Apache License v2.0
requiring this NOTICE file to be provided.


Portions of this software (C) Copyright 2010-2016 Alfresco Software, Ltd, licensed under the Apache License v2.0.

The following Flowable software libraries are distributed under the Apache License Version 2.0 (the License):

<#assign aggregation = aggregateLicenses(dependencyMap)>
<#list aggregation.flowable?sort as flowableArtifact>
    * org.flowable:${flowableArtifact}
</#list>

You may not use these files except in compliance with the License.  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an AS IS BASIS, WITHOUT WARRANTIES OR CONDITIONS OR ANY KIND, either express or implied.  See the License for the specific language governing permissions and limitations under the License.
This software package also includes third party components as listed below.


This software package includes changed source code of the following libraries:
Quartz
* Location: http://www.quartz-scheduler.org/
* CronExpression is included in flowable-engine-&lt;version&gt;.jar in package org.flowable.engine.impl.calendar
* License: Apache V2

This software includes unchanged copies of the following libraries grouped by their license type.

<#assign aggregate = aggregation.aggregate>
<#-- Print sorted aggregate licenses -->
<#list aggregate?keys?sort as licenses>
    <#assign projects = aggregate[licenses]/>

    ${licenses}

<#-- Print sorded projects -->
    <#list projects?sort as project>
        * ${project}
    </#list>
</#list>
