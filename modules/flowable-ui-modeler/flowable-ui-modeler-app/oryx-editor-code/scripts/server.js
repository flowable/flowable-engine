/**
 * Copyright (c) 2006
 * 
 * Philipp Berger, Martin Czuchra, Gero Decker, Ole Eckermann, Lutz Gericke,
 * Alexander Hold, Alexander Koglin, Oliver Kopp, Stefan Krumnow, 
 * Matthias Kunze, Philipp Maschke, Falko Menge, Christoph Neijenhuis, 
 * Hagen Overdick, Zhen Peng, Nicolas Peters, Kerstin Pfitzner, Daniel Polak,
 * Steffen Ryll, Kai Schlichting, Jan-Felix Schwarz, Daniel Taschik, 
 * Willi Tscheschner, Björn Wagner, Sven Wagner-Boysen, Matthias Weidlich
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 * 
 **/

if(!ORYX) var ORYX = {};

if(!ORYX.CONFIG) ORYX.CONFIG = {};

/**
 * This file contains URI constants that may be used for XMLHTTPRequests.
 */

ORYX.CONFIG.ROOT_PATH =					"editor/"; //TODO: Remove last slash!!
ORYX.CONFIG.EXPLORER_PATH =				"explorer";
ORYX.CONFIG.LIBS_PATH =					"libs";

/**
 * Regular Config
 */	
ORYX.CONFIG.SERVER_HANDLER_ROOT = 			"service";
ORYX.CONFIG.SERVER_EDITOR_HANDLER =			ORYX.CONFIG.SERVER_HANDLER_ROOT + "/editor";
ORYX.CONFIG.SERVER_MODEL_HANDLER =			ORYX.CONFIG.SERVER_HANDLER_ROOT + "/model";
ORYX.CONFIG.STENCILSET_HANDLER = 			ORYX.CONFIG.SERVER_HANDLER_ROOT + "/editor_stencilset?embedsvg=true&url=true&namespace=";    
ORYX.CONFIG.STENCIL_SETS_URL = 				ORYX.CONFIG.SERVER_HANDLER_ROOT + "/editor_stencilset";

ORYX.CONFIG.PLUGINS_CONFIG =				"editor-app/plugins.xml";
ORYX.CONFIG.SYNTAXCHECKER_URL =				ORYX.CONFIG.SERVER_HANDLER_ROOT + "/syntaxchecker";
ORYX.CONFIG.DEPLOY_URL = 					ORYX.CONFIG.SERVER_HANDLER_ROOT + "/model/deploy";
ORYX.CONFIG.MODEL_LIST_URL = 				ORYX.CONFIG.SERVER_HANDLER_ROOT + "/models";
ORYX.CONFIG.FORM_FLOW_LIST_URL = 			ORYX.CONFIG.SERVER_HANDLER_ROOT + "/formflows";
ORYX.CONFIG.FORM_FLOW_IMAGE_URL = 			ORYX.CONFIG.SERVER_HANDLER_ROOT + "/formflow";
ORYX.CONFIG.FORM_LIST_URL = 				ORYX.CONFIG.SERVER_HANDLER_ROOT + "/forms";
ORYX.CONFIG.FORM_IMAGE_URL = 				ORYX.CONFIG.SERVER_HANDLER_ROOT + "/form";
ORYX.CONFIG.SUB_PROCESS_LIST_URL = 			ORYX.CONFIG.SERVER_HANDLER_ROOT + "/subprocesses";
ORYX.CONFIG.SUB_PROCESS_IMAGE_URL = 		ORYX.CONFIG.SERVER_HANDLER_ROOT + "/subprocess";
ORYX.CONFIG.TEST_SERVICE_URL = 				ORYX.CONFIG.SERVER_HANDLER_ROOT + "/service/";

ORYX.CONFIG.SERVICE_LIST_URL = 				ORYX.CONFIG.SERVER_HANDLER_ROOT + "/services";
ORYX.CONFIG.CONDITION_ELEMENT_LIST_URL = 	ORYX.CONFIG.SERVER_HANDLER_ROOT + "/conditionelements";
ORYX.CONFIG.VARIABLEDEF_ELEMENT_LIST_URL = 	ORYX.CONFIG.SERVER_HANDLER_ROOT + "/variabledefinitionelements";
ORYX.CONFIG.VALIDATOR_LIST_URL = 			ORYX.CONFIG.SERVER_HANDLER_ROOT + "/validators";

ORYX.CONFIG.SS_EXTENSIONS_FOLDER =			ORYX.CONFIG.ROOT_PATH + "stencilsets/extensions/";
ORYX.CONFIG.SS_EXTENSIONS_CONFIG =			ORYX.CONFIG.SERVER_HANDLER_ROOT + "/editor_ssextensions";	
ORYX.CONFIG.ORYX_NEW_URL =					"/new";	
ORYX.CONFIG.BPMN_LAYOUTER =					ORYX.CONFIG.ROOT_PATH + "bpmnlayouter";

ORYX.CONFIG.EXPRESSION_METADATA_URL = 			ORYX.CONFIG.SERVER_HANDLER_ROOT + "/expression-metadata";
ORYX.CONFIG.DATASOURCE_METADATA_URL = 			ORYX.CONFIG.SERVER_HANDLER_ROOT + "/datasource-metadata";