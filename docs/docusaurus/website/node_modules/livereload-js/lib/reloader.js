(function() {
  var IMAGE_STYLES, Reloader, numberOfMatchingSegments, pathFromUrl, pathsMatch, pickBestMatch, splitUrl;

  splitUrl = function(url) {
    var comboSign, hash, index, params;
    if ((index = url.indexOf('#')) >= 0) {
      hash = url.slice(index);
      url = url.slice(0, index);
    } else {
      hash = '';
    }
    comboSign = url.indexOf('??');
    if (comboSign >= 0) {
      if (comboSign + 1 !== url.lastIndexOf('?')) {
        index = url.lastIndexOf('?');
      }
    } else {
      index = url.indexOf('?');
    }
    if (index >= 0) {
      params = url.slice(index);
      url = url.slice(0, index);
    } else {
      params = '';
    }
    return {
      url: url,
      params: params,
      hash: hash
    };
  };

  pathFromUrl = function(url) {
    var path;
    url = splitUrl(url).url;
    if (url.indexOf('file://') === 0) {
      path = url.replace(/^file:\/\/(localhost)?/, '');
    } else {
      path = url.replace(/^([^:]+:)?\/\/([^:\/]+)(:\d*)?\//, '/');
    }
    return decodeURIComponent(path);
  };

  pickBestMatch = function(path, objects, pathFunc) {
    var bestMatch, i, len1, object, score;
    bestMatch = {
      score: 0
    };
    for (i = 0, len1 = objects.length; i < len1; i++) {
      object = objects[i];
      score = numberOfMatchingSegments(path, pathFunc(object));
      if (score > bestMatch.score) {
        bestMatch = {
          object: object,
          score: score
        };
      }
    }
    if (bestMatch.score > 0) {
      return bestMatch;
    } else {
      return null;
    }
  };

  numberOfMatchingSegments = function(path1, path2) {
    var comps1, comps2, eqCount, len;
    path1 = path1.replace(/^\/+/, '').toLowerCase();
    path2 = path2.replace(/^\/+/, '').toLowerCase();
    if (path1 === path2) {
      return 10000;
    }
    comps1 = path1.split('/').reverse();
    comps2 = path2.split('/').reverse();
    len = Math.min(comps1.length, comps2.length);
    eqCount = 0;
    while (eqCount < len && comps1[eqCount] === comps2[eqCount]) {
      ++eqCount;
    }
    return eqCount;
  };

  pathsMatch = function(path1, path2) {
    return numberOfMatchingSegments(path1, path2) > 0;
  };

  IMAGE_STYLES = [
    {
      selector: 'background',
      styleNames: ['backgroundImage']
    }, {
      selector: 'border',
      styleNames: ['borderImage', 'webkitBorderImage', 'MozBorderImage']
    }
  ];

  exports.Reloader = Reloader = (function() {
    function Reloader(window, console, Timer) {
      this.window = window;
      this.console = console;
      this.Timer = Timer;
      this.document = this.window.document;
      this.importCacheWaitPeriod = 200;
      this.plugins = [];
    }

    Reloader.prototype.addPlugin = function(plugin) {
      return this.plugins.push(plugin);
    };

    Reloader.prototype.analyze = function(callback) {
      return results;
    };

    Reloader.prototype.reload = function(path, options) {
      var base, i, len1, plugin, ref;
      this.options = options;
      if ((base = this.options).stylesheetReloadTimeout == null) {
        base.stylesheetReloadTimeout = 15000;
      }
      ref = this.plugins;
      for (i = 0, len1 = ref.length; i < len1; i++) {
        plugin = ref[i];
        if (plugin.reload && plugin.reload(path, options)) {
          return;
        }
      }
      if (options.liveCSS && path.match(/\.css(?:\.map)?$/i)) {
        if (this.reloadStylesheet(path)) {
          return;
        }
      }
      if (options.liveImg && path.match(/\.(jpe?g|png|gif)$/i)) {
        this.reloadImages(path);
        return;
      }
      if (options.isChromeExtension) {
        this.reloadChromeExtension();
        return;
      }
      return this.reloadPage();
    };

    Reloader.prototype.reloadPage = function() {
      return this.window.document.location.reload();
    };

    Reloader.prototype.reloadChromeExtension = function() {
      return this.window.chrome.runtime.reload();
    };

    Reloader.prototype.reloadImages = function(path) {
      var expando, i, img, j, k, len1, len2, len3, len4, m, ref, ref1, ref2, ref3, results1, selector, styleNames, styleSheet;
      expando = this.generateUniqueString();
      ref = this.document.images;
      for (i = 0, len1 = ref.length; i < len1; i++) {
        img = ref[i];
        if (pathsMatch(path, pathFromUrl(img.src))) {
          img.src = this.generateCacheBustUrl(img.src, expando);
        }
      }
      if (this.document.querySelectorAll) {
        for (j = 0, len2 = IMAGE_STYLES.length; j < len2; j++) {
          ref1 = IMAGE_STYLES[j], selector = ref1.selector, styleNames = ref1.styleNames;
          ref2 = this.document.querySelectorAll("[style*=" + selector + "]");
          for (k = 0, len3 = ref2.length; k < len3; k++) {
            img = ref2[k];
            this.reloadStyleImages(img.style, styleNames, path, expando);
          }
        }
      }
      if (this.document.styleSheets) {
        ref3 = this.document.styleSheets;
        results1 = [];
        for (m = 0, len4 = ref3.length; m < len4; m++) {
          styleSheet = ref3[m];
          results1.push(this.reloadStylesheetImages(styleSheet, path, expando));
        }
        return results1;
      }
    };

    Reloader.prototype.reloadStylesheetImages = function(styleSheet, path, expando) {
      var e, error, i, j, len1, len2, rule, rules, styleNames;
      try {
        rules = styleSheet != null ? styleSheet.cssRules : void 0;
      } catch (error) {
        e = error;
      }
      if (!rules) {
        return;
      }
      for (i = 0, len1 = rules.length; i < len1; i++) {
        rule = rules[i];
        switch (rule.type) {
          case CSSRule.IMPORT_RULE:
            this.reloadStylesheetImages(rule.styleSheet, path, expando);
            break;
          case CSSRule.STYLE_RULE:
            for (j = 0, len2 = IMAGE_STYLES.length; j < len2; j++) {
              styleNames = IMAGE_STYLES[j].styleNames;
              this.reloadStyleImages(rule.style, styleNames, path, expando);
            }
            break;
          case CSSRule.MEDIA_RULE:
            this.reloadStylesheetImages(rule, path, expando);
        }
      }
    };

    Reloader.prototype.reloadStyleImages = function(style, styleNames, path, expando) {
      var i, len1, newValue, styleName, value;
      for (i = 0, len1 = styleNames.length; i < len1; i++) {
        styleName = styleNames[i];
        value = style[styleName];
        if (typeof value === 'string') {
          newValue = value.replace(/\burl\s*\(([^)]*)\)/, (function(_this) {
            return function(match, src) {
              if (pathsMatch(path, pathFromUrl(src))) {
                return "url(" + (_this.generateCacheBustUrl(src, expando)) + ")";
              } else {
                return match;
              }
            };
          })(this));
          if (newValue !== value) {
            style[styleName] = newValue;
          }
        }
      }
    };

    Reloader.prototype.reloadStylesheet = function(path) {
      var i, imported, j, k, len1, len2, len3, len4, link, links, m, match, ref, ref1, style;
      links = (function() {
        var i, len1, ref, results1;
        ref = this.document.getElementsByTagName('link');
        results1 = [];
        for (i = 0, len1 = ref.length; i < len1; i++) {
          link = ref[i];
          if (link.rel.match(/^stylesheet$/i) && !link.__LiveReload_pendingRemoval) {
            results1.push(link);
          }
        }
        return results1;
      }).call(this);
      imported = [];
      ref = this.document.getElementsByTagName('style');
      for (i = 0, len1 = ref.length; i < len1; i++) {
        style = ref[i];
        if (style.sheet) {
          this.collectImportedStylesheets(style, style.sheet, imported);
        }
      }
      for (j = 0, len2 = links.length; j < len2; j++) {
        link = links[j];
        this.collectImportedStylesheets(link, link.sheet, imported);
      }
      if (this.window.StyleFix && this.document.querySelectorAll) {
        ref1 = this.document.querySelectorAll('style[data-href]');
        for (k = 0, len3 = ref1.length; k < len3; k++) {
          style = ref1[k];
          links.push(style);
        }
      }
      this.console.log("LiveReload found " + links.length + " LINKed stylesheets, " + imported.length + " @imported stylesheets");
      match = pickBestMatch(path, links.concat(imported), (function(_this) {
        return function(l) {
          return pathFromUrl(_this.linkHref(l));
        };
      })(this));
      if (match) {
        if (match.object.rule) {
          this.console.log("LiveReload is reloading imported stylesheet: " + match.object.href);
          this.reattachImportedRule(match.object);
        } else {
          this.console.log("LiveReload is reloading stylesheet: " + (this.linkHref(match.object)));
          this.reattachStylesheetLink(match.object);
        }
      } else {
        if (this.options.reloadMissingCSS) {
          this.console.log("LiveReload will reload all stylesheets because path '" + path + "' did not match any specific one. To disable this behavior, set 'options.reloadMissingCSS' to 'false'.");
          for (m = 0, len4 = links.length; m < len4; m++) {
            link = links[m];
            this.reattachStylesheetLink(link);
          }
        } else {
          this.console.log("LiveReload will not reload path '" + path + "' because the stylesheet was not found on the page and 'options.reloadMissingCSS' was set to 'false'.");
        }
      }
      return true;
    };

    Reloader.prototype.collectImportedStylesheets = function(link, styleSheet, result) {
      var e, error, i, index, len1, rule, rules;
      try {
        rules = styleSheet != null ? styleSheet.cssRules : void 0;
      } catch (error) {
        e = error;
      }
      if (rules && rules.length) {
        for (index = i = 0, len1 = rules.length; i < len1; index = ++i) {
          rule = rules[index];
          switch (rule.type) {
            case CSSRule.CHARSET_RULE:
              continue;
            case CSSRule.IMPORT_RULE:
              result.push({
                link: link,
                rule: rule,
                index: index,
                href: rule.href
              });
              this.collectImportedStylesheets(link, rule.styleSheet, result);
              break;
            default:
              break;
          }
        }
      }
    };

    Reloader.prototype.waitUntilCssLoads = function(clone, func) {
      var callbackExecuted, executeCallback, poll;
      callbackExecuted = false;
      executeCallback = (function(_this) {
        return function() {
          if (callbackExecuted) {
            return;
          }
          callbackExecuted = true;
          return func();
        };
      })(this);
      clone.onload = (function(_this) {
        return function() {
          _this.console.log("LiveReload: the new stylesheet has finished loading");
          _this.knownToSupportCssOnLoad = true;
          return executeCallback();
        };
      })(this);
      if (!this.knownToSupportCssOnLoad) {
        (poll = (function(_this) {
          return function() {
            if (clone.sheet) {
              _this.console.log("LiveReload is polling until the new CSS finishes loading...");
              return executeCallback();
            } else {
              return _this.Timer.start(50, poll);
            }
          };
        })(this))();
      }
      return this.Timer.start(this.options.stylesheetReloadTimeout, executeCallback);
    };

    Reloader.prototype.linkHref = function(link) {
      return link.href || link.getAttribute('data-href');
    };

    Reloader.prototype.reattachStylesheetLink = function(link) {
      var clone, parent;
      if (link.__LiveReload_pendingRemoval) {
        return;
      }
      link.__LiveReload_pendingRemoval = true;
      if (link.tagName === 'STYLE') {
        clone = this.document.createElement('link');
        clone.rel = 'stylesheet';
        clone.media = link.media;
        clone.disabled = link.disabled;
      } else {
        clone = link.cloneNode(false);
      }
      clone.href = this.generateCacheBustUrl(this.linkHref(link));
      parent = link.parentNode;
      if (parent.lastChild === link) {
        parent.appendChild(clone);
      } else {
        parent.insertBefore(clone, link.nextSibling);
      }
      return this.waitUntilCssLoads(clone, (function(_this) {
        return function() {
          var additionalWaitingTime;
          if (/AppleWebKit/.test(navigator.userAgent)) {
            additionalWaitingTime = 5;
          } else {
            additionalWaitingTime = 200;
          }
          return _this.Timer.start(additionalWaitingTime, function() {
            var ref;
            if (!link.parentNode) {
              return;
            }
            link.parentNode.removeChild(link);
            clone.onreadystatechange = null;
            return (ref = _this.window.StyleFix) != null ? ref.link(clone) : void 0;
          });
        };
      })(this));
    };

    Reloader.prototype.reattachImportedRule = function(arg) {
      var href, index, link, media, newRule, parent, rule, tempLink;
      rule = arg.rule, index = arg.index, link = arg.link;
      parent = rule.parentStyleSheet;
      href = this.generateCacheBustUrl(rule.href);
      media = rule.media.length ? [].join.call(rule.media, ', ') : '';
      newRule = "@import url(\"" + href + "\") " + media + ";";
      rule.__LiveReload_newHref = href;
      tempLink = this.document.createElement("link");
      tempLink.rel = 'stylesheet';
      tempLink.href = href;
      tempLink.__LiveReload_pendingRemoval = true;
      if (link.parentNode) {
        link.parentNode.insertBefore(tempLink, link);
      }
      return this.Timer.start(this.importCacheWaitPeriod, (function(_this) {
        return function() {
          if (tempLink.parentNode) {
            tempLink.parentNode.removeChild(tempLink);
          }
          if (rule.__LiveReload_newHref !== href) {
            return;
          }
          parent.insertRule(newRule, index);
          parent.deleteRule(index + 1);
          rule = parent.cssRules[index];
          rule.__LiveReload_newHref = href;
          return _this.Timer.start(_this.importCacheWaitPeriod, function() {
            if (rule.__LiveReload_newHref !== href) {
              return;
            }
            parent.insertRule(newRule, index);
            return parent.deleteRule(index + 1);
          });
        };
      })(this));
    };

    Reloader.prototype.generateUniqueString = function() {
      return 'livereload=' + Date.now();
    };

    Reloader.prototype.generateCacheBustUrl = function(url, expando) {
      var hash, oldParams, originalUrl, params, ref;
      if (expando == null) {
        expando = this.generateUniqueString();
      }
      ref = splitUrl(url), url = ref.url, hash = ref.hash, oldParams = ref.params;
      if (this.options.overrideURL) {
        if (url.indexOf(this.options.serverURL) < 0) {
          originalUrl = url;
          url = this.options.serverURL + this.options.overrideURL + "?url=" + encodeURIComponent(url);
          this.console.log("LiveReload is overriding source URL " + originalUrl + " with " + url);
        }
      }
      params = oldParams.replace(/(\?|&)livereload=(\d+)/, function(match, sep) {
        return "" + sep + expando;
      });
      if (params === oldParams) {
        if (oldParams.length === 0) {
          params = "?" + expando;
        } else {
          params = oldParams + "&" + expando;
        }
      }
      return url + params + hash;
    };

    return Reloader;

  })();

}).call(this);
