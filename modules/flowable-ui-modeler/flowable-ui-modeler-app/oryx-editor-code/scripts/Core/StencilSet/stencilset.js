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

/**
 * Init namespace
 */
if (!ORYX) {
    var ORYX = {};
}
if (!ORYX.Core) {
    ORYX.Core = {};
}
if (!ORYX.Core.StencilSet) {
    ORYX.Core.StencilSet = {};
}

/**
 * This class represents a stencil set. It offers methods for accessing
 *  the attributes of the stencil set description JSON file and the stencil set's
 *  stencils.
 */
ORYX.Core.StencilSet.StencilSet = Clazz.extend({

    /**
     * Constructor
     * @param source {URL} A reference to the stencil set specification.
     *
     */
    construct: function(baseUrl, data) {
		
		this._extensions = new Hash();
        
        this._baseUrl = baseUrl;
        this._jsonObject = {};
        
        this._stencils = new Hash();
		this._availableStencils = new Hash();
       	this._init(data);
    },
    
    /**
     * Finds a root stencil in this stencil set. There may be many of these. If
     * there are, the first one found will be used. In Firefox, this is the
     * topmost definition in the stencil set description file.
     */
    findRootStencilName: function(){
    
        // find any stencil that may be root.
        var rootStencil = this._stencils.values().find(function(stencil){
            return stencil._jsonStencil.mayBeRoot
        });
        
		// if there is none, just guess the first.
		if (!rootStencil) {
			ORYX.Log.warn("Did not find any stencil that may be root. Taking a guess.");
			rootStencil = this._stencils.values()[0];
		}

        // return its id.
        return rootStencil.id();
    },
    
    /**
     * @param {ORYX.Core.StencilSet.StencilSet} stencilSet
     * @return {Boolean} True, if stencil set has the same namespace.
     */
    equals: function(stencilSet){
        return (this.namespace() === stencilSet.namespace());
    },
    
	/**
	 * 
	 * @param {Oryx.Core.StencilSet.Stencil} rootStencil If rootStencil is defined, it only returns stencils
	 * 			that could be (in)direct child of that stencil.
	 */
    stencils: function(rootStencil, rules, sortByGroup){
		if(rootStencil && rules) {
			var stencils = this._availableStencils.values();
			var containers = [rootStencil];
			var checkedContainers = [];
			
			var result = [];
			
			while (containers.size() > 0) {
				var container = containers.pop();
				checkedContainers.push(container);
				var children = stencils.findAll(function(stencil){
					var args = {
						containingStencil: container,
						containedStencil: stencil
					};
					return rules.canContain(args);
				});
				for(var i = 0; i < children.size(); i++) {
					if (!checkedContainers.member(children[i])) {
						containers.push(children[i]);
					}
				}
				result = result.concat(children).uniq();
			}
			
			// Sort the result to the origin order
			result = result.sortBy(function(stencil) {
				return stencils.indexOf(stencil);
			});
			
			
			if(sortByGroup) {
				result = result.sortBy(function(stencil) {
					return stencil.groups().first();
				});
			}
			
			var edges = stencils.findAll(function(stencil) {
				return stencil.type() == "edge";
			});
			result = result.concat(edges);
			
			return result;
			
		} else {
        	if(sortByGroup) {
				return this._availableStencils.values().sortBy(function(stencil) {
					return stencil.groups().first();
				});
			} else {
				return this._availableStencils.values();
			}
		}
    },
    
    nodes: function(){
        return this._availableStencils.values().findAll(function(stencil){
            return (stencil.type() === 'node')
        });
    },
    
    edges: function(){
        return this._availableStencils.values().findAll(function(stencil){
            return (stencil.type() === 'edge')
        });
    },
    
    stencil: function(id){
        return this._stencils.get(id);
    },
    
    title: function(){
        return ORYX.Core.StencilSet.getTranslation(this._jsonObject, "title");
    },
    
    description: function(){
        return ORYX.Core.StencilSet.getTranslation(this._jsonObject, "description");
    },
    
    namespace: function(){
        return this._jsonObject ? this._jsonObject.namespace : null;
    },
    
    jsonRules: function(){
        return this._jsonObject ? this._jsonObject.rules : null;
    },
    
    source: function(){
        return this._source;
    },
	
	extensions: function() {
		return this._extensions;
	},
	
	addExtension: function(url) {
		
		new Ajax.Request(url, {
            method: 'GET',
            asynchronous: false,
			onSuccess: (function(transport) {
				this.addExtensionDirectly(transport.responseText);
			}).bind(this),
			onFailure: (function(transport) {
				ORYX.Log.debug("Loading stencil set extension file failed. The request returned an error." + transport);
			}).bind(this),
			onException: (function(transport) {
				ORYX.Log.debug("Loading stencil set extension file failed. The request returned an error." + transport);
			}).bind(this)
		
		});
	},
	
	addExtensionDirectly: function(str){

		try {
			eval("var jsonExtension = " + str);

			if(!(jsonExtension["extends"].endsWith("#")))
					jsonExtension["extends"] += "#";
					
			if(jsonExtension["extends"] == this.namespace()) {
				this._extensions.set(jsonExtension.namespace, jsonExtension);
				
				var defaultPosition = this._stencils.keys().size();
				//load new stencils
				if(jsonExtension.stencils) {
					$A(jsonExtension.stencils).each(function(stencil) {
						defaultPosition++;
						var oStencil = new ORYX.Core.StencilSet.Stencil(stencil, this.namespace(), this._baseUrl, this, undefined, defaultPosition);            
						this._stencils.set(oStencil.id(),oStencil);
						this._availableStencils.set(oStencil.id(),oStencil);
					}.bind(this));
				}
				
				//load additional properties
				if (jsonExtension.properties) {
					var stencils = this._stencils.values();
					
					stencils.each(function(stencil){
						var roles = stencil.roles();
						
						jsonExtension.properties.each(function(prop){
							prop.roles.any(function(role){
								role = jsonExtension["extends"] + role;
								if (roles.member(role)) {
									prop.properties.each(function(property){
										stencil.addProperty(property, jsonExtension.namespace);
									});
									
									return true;
								}
								else 
									return false;
							})
						})
					}.bind(this));
				}
				
				//remove stencil properties
				if(jsonExtension.removeproperties) {
					jsonExtension.removeproperties.each(function(remprop) {
						var stencil = this.stencil(jsonExtension["extends"] + remprop.stencil);
						if(stencil) {
							remprop.properties.each(function(propId) {
								stencil.removeProperty(propId);
							});
						}
					}.bind(this));
				}
				
				//remove stencils
				if(jsonExtension.removestencils) {
					$A(jsonExtension.removestencils).each(function(remstencil) {
						delete this._availableStencils[jsonExtension["extends"] + remstencil];
					}.bind(this));
				}
			}
		} catch (e) {
			ORYX.Log.debug("StencilSet.addExtension: Something went wrong when initialising the stencil set extension. " + e);
		}	
	},
	
	removeExtension: function(namespace) {
		var jsonExtension = this._extensions[namespace];
		if(jsonExtension) {
			
			//unload extension's stencils
			if(jsonExtension.stencils) {
				$A(jsonExtension.stencils).each(function(stencil) {
					var oStencil = new ORYX.Core.StencilSet.Stencil(stencil, this.namespace(), this._baseUrl, this);            
					this._stencils.unset(oStencil.id());
					this._availableStencils.unset(oStencil.id());
				}.bind(this));
			}
			
			//unload extension's properties
			if (jsonExtension.properties) {
				var stencils = this._stencils.values();
				
				stencils.each(function(stencil){
					var roles = stencil.roles();
					
					jsonExtension.properties.each(function(prop){
						prop.roles.any(function(role){
							role = jsonExtension["extends"] + role;
							if (roles.member(role)) {
								prop.properties.each(function(property){
									stencil.removeProperty(property.id);
								});
								
								return true;
							}
							else 
								return false;
						})
					})
				}.bind(this));
			}
			
			//restore removed stencil properties
			if(jsonExtension.removeproperties) {
				jsonExtension.removeproperties.each(function(remprop) {
					var stencil = this.stencil(jsonExtension["extends"] + remprop.stencil);
					if(stencil) {
						var stencilJson = $A(this._jsonObject.stencils).find(function(s) { return s.id == stencil.id() });
						remprop.properties.each(function(propId) {
							var propertyJson = $A(stencilJson.properties).find(function(p) { return p.id == propId });
							stencil.addProperty(propertyJson, this.namespace());
						}.bind(this));
					}
				}.bind(this));
			}
			
			//restore removed stencils
			if(jsonExtension.removestencils) {
				$A(jsonExtension.removestencils).each(function(remstencil) {
					var sId = jsonExtension["extends"] + remstencil;
					this._availableStencils.set(sId, this._stencils.get(sId));
				}.bind(this));
			}
		}
		delete this._extensions[namespace];
	},
    
    __handleStencilset: function(response){
    
        this._jsonObject = response;
        
        // assert it was parsed.
        if (!this._jsonObject) {
            throw "Error evaluating stencilset. It may be corrupt.";
        }
        
        with (this._jsonObject) {
        
            // assert there is a namespace.
            if (!namespace || namespace === "") 
                throw "Namespace definition missing in stencilset.";
            
            if (!(stencils instanceof Array)) 
                throw "Stencilset corrupt.";
            
            // assert namespace ends with '#'.
            if (!namespace.endsWith("#")) 
                namespace = namespace + "#";
            
            // assert title and description are strings.
            if (!title) 
                title = "";
            if (!description) 
                description = "";
        }
    },
    
    /**
     * This method is called when the HTTP request to get the requested stencil
     * set succeeds. The response is supposed to be a JSON representation
     * according to the stencil set specification.
     * @param {Object} response The JSON representation according to the
     * 			stencil set specification.
     */
    _init: function(response){
    
        // init and check consistency.
        this.__handleStencilset(response);
		
		var pps = new Hash();
		
		// init property packages
		if(this._jsonObject.propertyPackages) {
			$A(this._jsonObject.propertyPackages).each((function(pp) {
				pps.set(pp.name, pp.properties);
			}).bind(this));
		}
		
		var defaultPosition = 0;
		
    
    if(!customTaskConfigs) {
      new Ajax.Request(
          FLOWABLE.CONFIG.contextRoot + "/app/workflow/model/taskconfigs", {
            asynchronous:false, method:'get',
            onSuccess:function(result) {
              customTaskConfigs=result.responseText.evalJSON();
              },
            onFailure:function(result) {}
        });           
    }
    
    // rds - start added by simon to handle custom task 
      for (var i = 0; i < customTaskConfigs.length; i++) {
          var taskConfig = customTaskConfigs[i]; 
          var taskclasspackage = taskConfig.id+"implpackage";     
          
          if(taskConfig.type==="class") {         
            pps.set(taskclasspackage, [ {
             "id" : "servicetaskclass",
             "type" : "String",
             "title" : "Class",
             "value" : taskConfig.implementation,
             "description" : "Class that implements the service task logic.",
             "popular" : !taskConfig.hideImpl            
            } ]);
      } else if(taskConfig.type==="expression") {
        pps.set(taskclasspackage, [ {
               "id" : "servicetaskexpression",
               "type" : "String",
               "title" : "Expression",
               "value" : taskConfig.implementation,
               "description" : "Service task logic defined with an expression.",
               "popular" : !taskConfig.hideImpl
              } ]);
      } else {
        pps.set(taskclasspackage, [ {
               "id" : "servicetaskdelegateexpression",
               "type" : "String",
               "title" : "Delegate Expression",
               "value" : taskConfig.implementation,
               "description" : "Service task logic defined with a delegate expression.",
               "popular" : !taskConfig.hideImpl
              } ]);
      }
          
      var paras = [];
      if(taskConfig.parameters) {
        for (var j = 0; j < taskConfig.parameters.length; j++) {
          var para = taskConfig.parameters[j];
          if (para.type==="expression" ) {
            paras.push({"name":para.name,"expression":para.value,"label":para.label});
          } else {
            paras.push({"name":para.name,"string":para.value,"label":para.label});
          }
        }
      }
          
      pps.set("servicetask-field-"+taskConfig.id, [{
           "id" : "servicetaskfields",
           "type" : "Complex",
           "title" : "Parameters",
           "value" : {"fields":paras},
           "description" : "Parameters",
           "popular" : !taskConfig.hideParameters 
      }]);
                
      var customIconSVG = '<image x="2" y="2" width="18" height="18" xlink:href="'+ FLOWABLE.CONFIG.contextRoot + '/editor-app/stencilsets/bpmn2.0/icons/'+ taskConfig.iconUrl+'" />'
      var view = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<svg\n   xmlns=\"http://www.w3.org/2000/svg\"\n   xmlns:svg=\"http://www.w3.org/2000/svg\"\n   xmlns:oryx=\"http://www.b3mn.org/oryx\"\n   xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n\n   width=\"102\"\n   height=\"82\"\n   version=\"1.0\">\n  <defs></defs>\n  <oryx:magnets>\n  \t<oryx:magnet oryx:cx=\"1\" oryx:cy=\"20\" oryx:anchors=\"left\" />\n  \t<oryx:magnet oryx:cx=\"1\" oryx:cy=\"40\" oryx:anchors=\"left\" />\n  \t<oryx:magnet oryx:cx=\"1\" oryx:cy=\"60\" oryx:anchors=\"left\" />\n  \t\n  \t<oryx:magnet oryx:cx=\"25\" oryx:cy=\"79\" oryx:anchors=\"bottom\" />\n  \t<oryx:magnet oryx:cx=\"50\" oryx:cy=\"79\" oryx:anchors=\"bottom\" />\n  \t<oryx:magnet oryx:cx=\"75\" oryx:cy=\"79\" oryx:anchors=\"bottom\" />\n  \t\n  \t<oryx:magnet oryx:cx=\"99\" oryx:cy=\"20\" oryx:anchors=\"right\" />\n  \t<oryx:magnet oryx:cx=\"99\" oryx:cy=\"40\" oryx:anchors=\"right\" />\n  \t<oryx:magnet oryx:cx=\"99\" oryx:cy=\"60\" oryx:anchors=\"right\" />\n  \t\n  \t<oryx:magnet oryx:cx=\"25\" oryx:cy=\"1\" oryx:anchors=\"top\" />\n  \t<oryx:magnet oryx:cx=\"50\" oryx:cy=\"1\" oryx:anchors=\"top\" />\n  \t<oryx:magnet oryx:cx=\"75\" oryx:cy=\"1\" oryx:anchors=\"top\" />\n  \t\n  \t<oryx:magnet oryx:cx=\"50\" oryx:cy=\"40\" oryx:default=\"yes\" />\n  </oryx:magnets>\n  <g pointer-events=\"fill\" oryx:minimumSize=\"50 40\">\n\t<rect id=\"text_frame\" oryx:anchors=\"bottom top right left\" x=\"1\" y=\"1\" width=\"94\" height=\"79\" rx=\"10\" ry=\"10\" stroke=\"none\" stroke-width=\"0\" fill=\"none\" />\n\t<rect id=\"bg_frame\" oryx:resize=\"vertical horizontal\" x=\"0\" y=\"0\" width=\"100\" height=\"80\" rx=\"10\" ry=\"10\" stroke=\"#bbbbbb\" stroke-width=\"1\" fill=\"#f9f9f9\" />\n\t\t<text \n\t\t\tfont-size=\"12\" \n\t\t\tid=\"text_name\" \n\t\t\tx=\"50\" \n\t\t\ty=\"40\" \n\t\t\toryx:align=\"middle center\"\n\t\t\toryx:fittoelem=\"text_frame\"\n\t\t\tstroke=\"#373e48\">\n\t\t</text>\n\t\n\t<g id=\"serviceTask\" transform=\"translate(3,3)\">\n\t"+ customIconSVG + "\n\t</g>\n  \n\t<g id=\"parallel\">\n\t\t<path oryx:anchors=\"bottom\" fill=\"none\" stroke=\"#bbbbbb\" d=\"M46 70 v8 M50 70 v8 M54 70 v8\" stroke-width=\"2\" />\n\t</g>\n\t\n\t<g id=\"sequential\">\n\t\t<path oryx:anchors=\"bottom\" fill=\"none\" stroke=\"#bbbbbb\" stroke-width=\"2\" d=\"M46,76h10M46,72h10 M46,68h10\"/>\n\t</g>\n\t\n\t<g id=\"compensation\">\n\t\t<path oryx:anchors=\"bottom\" fill=\"none\" stroke=\"#bbbbbb\" d=\"M 62 74 L 66 70 L 66 78 L 62 74 L 62 70 L 58 74 L 62 78 L 62 74\" stroke-width=\"1\" />\n\t</g>\n  </g>\n</svg>";
      if(taskConfig.svgIcon&&taskConfig.svgIcon.length>0) {
        view = taskConfig.svgIcon;
      }
      
        
      var customStencil = {
          "type" : "node",
          "id" : taskConfig.id,
          "title" : taskConfig.title,
          "description" : taskConfig.description,             
          "view" : view,
          "icon" : taskConfig.iconUrl,
          "groups" : [ taskConfig.group ],
          "propertyPackages" : [ "overrideidpackage", "namepackage", "documentationpackage", taskclasspackage, "servicetask-field-"+taskConfig.id, 'isforcompensationpackage', 'multiinstance_typepackage'],
          "hiddenPropertyPackages" : [],
          "roles" : [ "Activity", "sequence_start", "sequence_end", "ActivitiesMorph", "all" ]
      };
          
      paras.forEach(function(para) {
        pps.set("servicetask-field-"+taskConfig.id+"_"+para.name, [ {
          "id" : taskConfig.id + "-" + para.name,
          "type" : "Parameter",
          "title" : para.label,
          "value" : para.string,
          "description" : para.name,
          "popular" : true,
          "paraName": para.name
        }]);
            
        customStencil.propertyPackages.push("servicetask-field-"+taskConfig.id+"_"+para.name);
      });
        
      this._jsonObject.stencils.push(customStencil);
          
    }
    
    // rds - end added by simon to hanlde custom task type
    
        // init each stencil
        $A(this._jsonObject.stencils).each((function(stencil){
        	defaultPosition++;
        	
            // instantiate normally.
            var oStencil = new ORYX.Core.StencilSet.Stencil(stencil, this.namespace(), this._baseUrl, this, pps, defaultPosition);      
			this._stencils.set(oStencil.id(), oStencil);
			this._availableStencils.set(oStencil.id(), oStencil);
            
        }).bind(this));
    },
    
    _cancelInit: function(response){
        this.errornous = true;
    },
    
    toString: function(){
        return "StencilSet " + this.title() + " (" + this.namespace() + ")";
    }
});
