/* Copyright 2005-2015 Alfresco Software, Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
/**
 * 
 * Global fixed configuration values for Flowable Admin app.
 * 
 */
var FlowableAdmin = {};
FlowableAdmin.Config = {};

// General settings
FlowableAdmin.Config.alert = {};
FlowableAdmin.Config.alert.infoDisplayTime = 3000;
FlowableAdmin.Config.alert.errorDisplayTime = 5000;

// Filter settings
FlowableAdmin.Config.filter = {};
FlowableAdmin.Config.filter.resultSizes = [10, 25, 50, 100, 100000000];    // fairly large number for 'all'
FlowableAdmin.Config.filter.defaultResultSize = 25;
FlowableAdmin.Config.filter.defaultOrder = "asc";
FlowableAdmin.Config.filter.delay = 400;

// Localization
FlowableAdmin.Config.localization = {};
FlowableAdmin.Config.localization.dates = false;
