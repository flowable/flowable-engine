(function(){function r(e,n,t){function o(i,f){if(!n[i]){if(!e[i]){var c="function"==typeof require&&require;if(!f&&c)return c(i,!0);if(u)return u(i,!0);var a=new Error("Cannot find module '"+i+"'");throw a.code="MODULE_NOT_FOUND",a}var p=n[i]={exports:{}};e[i][0].call(p.exports,function(r){var n=e[i][1][r];return o(n||r)},p,p.exports,r,e,n,t)}return n[i].exports}for(var u="function"==typeof require&&require,i=0;i<t.length;i++)o(t[i]);return o}return r})()({1:[function(require,module,exports){
(function() {
  var Connector, PROTOCOL_6, PROTOCOL_7, Parser, Version, ref;

  ref = require('./protocol'), Parser = ref.Parser, PROTOCOL_6 = ref.PROTOCOL_6, PROTOCOL_7 = ref.PROTOCOL_7;

  Version = "2.4.0";

  exports.Connector = Connector = (function() {
    function Connector(options, WebSocket, Timer, handlers) {
      var path;
      this.options = options;
      this.WebSocket = WebSocket;
      this.Timer = Timer;
      this.handlers = handlers;
      path = this.options.path ? "" + this.options.path : "livereload";
      this._uri = "ws" + (this.options.https ? "s" : "") + "://" + this.options.host + ":" + this.options.port + "/" + path;
      this._nextDelay = this.options.mindelay;
      this._connectionDesired = false;
      this.protocol = 0;
      this.protocolParser = new Parser({
        connected: (function(_this) {
          return function(protocol) {
            _this.protocol = protocol;
            _this._handshakeTimeout.stop();
            _this._nextDelay = _this.options.mindelay;
            _this._disconnectionReason = 'broken';
            return _this.handlers.connected(_this.protocol);
          };
        })(this),
        error: (function(_this) {
          return function(e) {
            _this.handlers.error(e);
            return _this._closeOnError();
          };
        })(this),
        message: (function(_this) {
          return function(message) {
            return _this.handlers.message(message);
          };
        })(this)
      });
      this._handshakeTimeout = new this.Timer((function(_this) {
        return function() {
          if (!_this._isSocketConnected()) {
            return;
          }
          _this._disconnectionReason = 'handshake-timeout';
          return _this.socket.close();
        };
      })(this));
      this._reconnectTimer = new this.Timer((function(_this) {
        return function() {
          if (!_this._connectionDesired) {
            return;
          }
          return _this.connect();
        };
      })(this));
      this.connect();
    }

    Connector.prototype._isSocketConnected = function() {
      return this.socket && this.socket.readyState === this.WebSocket.OPEN;
    };

    Connector.prototype.connect = function() {
      this._connectionDesired = true;
      if (this._isSocketConnected()) {
        return;
      }
      this._reconnectTimer.stop();
      this._disconnectionReason = 'cannot-connect';
      this.protocolParser.reset();
      this.handlers.connecting();
      this.socket = new this.WebSocket(this._uri);
      this.socket.onopen = (function(_this) {
        return function(e) {
          return _this._onopen(e);
        };
      })(this);
      this.socket.onclose = (function(_this) {
        return function(e) {
          return _this._onclose(e);
        };
      })(this);
      this.socket.onmessage = (function(_this) {
        return function(e) {
          return _this._onmessage(e);
        };
      })(this);
      return this.socket.onerror = (function(_this) {
        return function(e) {
          return _this._onerror(e);
        };
      })(this);
    };

    Connector.prototype.disconnect = function() {
      this._connectionDesired = false;
      this._reconnectTimer.stop();
      if (!this._isSocketConnected()) {
        return;
      }
      this._disconnectionReason = 'manual';
      return this.socket.close();
    };

    Connector.prototype._scheduleReconnection = function() {
      if (!this._connectionDesired) {
        return;
      }
      if (!this._reconnectTimer.running) {
        this._reconnectTimer.start(this._nextDelay);
        return this._nextDelay = Math.min(this.options.maxdelay, this._nextDelay * 2);
      }
    };

    Connector.prototype.sendCommand = function(command) {
      if (this.protocol == null) {
        return;
      }
      return this._sendCommand(command);
    };

    Connector.prototype._sendCommand = function(command) {
      return this.socket.send(JSON.stringify(command));
    };

    Connector.prototype._closeOnError = function() {
      this._handshakeTimeout.stop();
      this._disconnectionReason = 'error';
      return this.socket.close();
    };

    Connector.prototype._onopen = function(e) {
      var hello;
      this.handlers.socketConnected();
      this._disconnectionReason = 'handshake-failed';
      hello = {
        command: 'hello',
        protocols: [PROTOCOL_6, PROTOCOL_7]
      };
      hello.ver = Version;
      if (this.options.ext) {
        hello.ext = this.options.ext;
      }
      if (this.options.extver) {
        hello.extver = this.options.extver;
      }
      if (this.options.snipver) {
        hello.snipver = this.options.snipver;
      }
      this._sendCommand(hello);
      return this._handshakeTimeout.start(this.options.handshake_timeout);
    };

    Connector.prototype._onclose = function(e) {
      this.protocol = 0;
      this.handlers.disconnected(this._disconnectionReason, this._nextDelay);
      return this._scheduleReconnection();
    };

    Connector.prototype._onerror = function(e) {};

    Connector.prototype._onmessage = function(e) {
      return this.protocolParser.process(e.data);
    };

    return Connector;

  })();

}).call(this);

},{"./protocol":6}],2:[function(require,module,exports){
(function() {
  var CustomEvents;

  CustomEvents = {
    bind: function(element, eventName, handler) {
      if (element.addEventListener) {
        return element.addEventListener(eventName, handler, false);
      } else if (element.attachEvent) {
        element[eventName] = 1;
        return element.attachEvent('onpropertychange', function(event) {
          if (event.propertyName === eventName) {
            return handler();
          }
        });
      } else {
        throw new Error("Attempt to attach custom event " + eventName + " to something which isn't a DOMElement");
      }
    },
    fire: function(element, eventName) {
      var event;
      if (element.addEventListener) {
        event = document.createEvent('HTMLEvents');
        event.initEvent(eventName, true, true);
        return document.dispatchEvent(event);
      } else if (element.attachEvent) {
        if (element[eventName]) {
          return element[eventName]++;
        }
      } else {
        throw new Error("Attempt to fire custom event " + eventName + " on something which isn't a DOMElement");
      }
    }
  };

  exports.bind = CustomEvents.bind;

  exports.fire = CustomEvents.fire;

}).call(this);

},{}],3:[function(require,module,exports){
(function() {
  var LessPlugin;

  module.exports = LessPlugin = (function() {
    LessPlugin.identifier = 'less';

    LessPlugin.version = '1.0';

    function LessPlugin(window, host) {
      this.window = window;
      this.host = host;
    }

    LessPlugin.prototype.reload = function(path, options) {
      if (this.window.less && this.window.less.refresh) {
        if (path.match(/\.less$/i)) {
          return this.reloadLess(path);
        }
        if (options.originalPath.match(/\.less$/i)) {
          return this.reloadLess(options.originalPath);
        }
      }
      return false;
    };

    LessPlugin.prototype.reloadLess = function(path) {
      var i, len, link, links;
      links = (function() {
        var i, len, ref, results;
        ref = document.getElementsByTagName('link');
        results = [];
        for (i = 0, len = ref.length; i < len; i++) {
          link = ref[i];
          if (link.href && link.rel.match(/^stylesheet\/less$/i) || (link.rel.match(/stylesheet/i) && link.type.match(/^text\/(x-)?less$/i))) {
            results.push(link);
          }
        }
        return results;
      })();
      if (links.length === 0) {
        return false;
      }
      for (i = 0, len = links.length; i < len; i++) {
        link = links[i];
        link.href = this.host.generateCacheBustUrl(link.href);
      }
      this.host.console.log("LiveReload is asking LESS to recompile all stylesheets");
      this.window.less.refresh(true);
      return true;
    };

    LessPlugin.prototype.analyze = function() {
      return {
        disable: !!(this.window.less && this.window.less.refresh)
      };
    };

    return LessPlugin;

  })();

}).call(this);

},{}],4:[function(require,module,exports){
(function() {
  var Connector, LiveReload, Options, ProtocolError, Reloader, Timer,
    hasProp = {}.hasOwnProperty;

  Connector = require('./connector').Connector;

  Timer = require('./timer').Timer;

  Options = require('./options').Options;

  Reloader = require('./reloader').Reloader;

  ProtocolError = require('./protocol').ProtocolError;

  exports.LiveReload = LiveReload = (function() {
    function LiveReload(window1) {
      var k, ref, v;
      this.window = window1;
      this.listeners = {};
      this.plugins = [];
      this.pluginIdentifiers = {};
      this.console = this.window.console && this.window.console.log && this.window.console.error ? this.window.location.href.match(/LR-verbose/) ? this.window.console : {
        log: function() {},
        error: this.window.console.error.bind(this.window.console)
      } : {
        log: function() {},
        error: function() {}
      };
      if (!(this.WebSocket = this.window.WebSocket || this.window.MozWebSocket)) {
        this.console.error("LiveReload disabled because the browser does not seem to support web sockets");
        return;
      }
      if ('LiveReloadOptions' in window) {
        this.options = new Options();
        ref = window['LiveReloadOptions'];
        for (k in ref) {
          if (!hasProp.call(ref, k)) continue;
          v = ref[k];
          this.options.set(k, v);
        }
      } else {
        this.options = Options.extract(this.window.document);
        if (!this.options) {
          this.console.error("LiveReload disabled because it could not find its own <SCRIPT> tag");
          return;
        }
      }
      this.reloader = new Reloader(this.window, this.console, Timer);
      this.connector = new Connector(this.options, this.WebSocket, Timer, {
        connecting: (function(_this) {
          return function() {};
        })(this),
        socketConnected: (function(_this) {
          return function() {};
        })(this),
        connected: (function(_this) {
          return function(protocol) {
            var base;
            if (typeof (base = _this.listeners).connect === "function") {
              base.connect();
            }
            _this.log("LiveReload is connected to " + _this.options.host + ":" + _this.options.port + " (protocol v" + protocol + ").");
            return _this.analyze();
          };
        })(this),
        error: (function(_this) {
          return function(e) {
            if (e instanceof ProtocolError) {
              if (typeof console !== "undefined" && console !== null) {
                return console.log(e.message + ".");
              }
            } else {
              if (typeof console !== "undefined" && console !== null) {
                return console.log("LiveReload internal error: " + e.message);
              }
            }
          };
        })(this),
        disconnected: (function(_this) {
          return function(reason, nextDelay) {
            var base;
            if (typeof (base = _this.listeners).disconnect === "function") {
              base.disconnect();
            }
            switch (reason) {
              case 'cannot-connect':
                return _this.log("LiveReload cannot connect to " + _this.options.host + ":" + _this.options.port + ", will retry in " + nextDelay + " sec.");
              case 'broken':
                return _this.log("LiveReload disconnected from " + _this.options.host + ":" + _this.options.port + ", reconnecting in " + nextDelay + " sec.");
              case 'handshake-timeout':
                return _this.log("LiveReload cannot connect to " + _this.options.host + ":" + _this.options.port + " (handshake timeout), will retry in " + nextDelay + " sec.");
              case 'handshake-failed':
                return _this.log("LiveReload cannot connect to " + _this.options.host + ":" + _this.options.port + " (handshake failed), will retry in " + nextDelay + " sec.");
              case 'manual':
                break;
              case 'error':
                break;
              default:
                return _this.log("LiveReload disconnected from " + _this.options.host + ":" + _this.options.port + " (" + reason + "), reconnecting in " + nextDelay + " sec.");
            }
          };
        })(this),
        message: (function(_this) {
          return function(message) {
            switch (message.command) {
              case 'reload':
                return _this.performReload(message);
              case 'alert':
                return _this.performAlert(message);
            }
          };
        })(this)
      });
      this.initialized = true;
    }

    LiveReload.prototype.on = function(eventName, handler) {
      return this.listeners[eventName] = handler;
    };

    LiveReload.prototype.log = function(message) {
      return this.console.log("" + message);
    };

    LiveReload.prototype.performReload = function(message) {
      var ref, ref1, ref2;
      this.log("LiveReload received reload request: " + (JSON.stringify(message, null, 2)));
      return this.reloader.reload(message.path, {
        liveCSS: (ref = message.liveCSS) != null ? ref : true,
        liveImg: (ref1 = message.liveImg) != null ? ref1 : true,
        reloadMissingCSS: (ref2 = message.reloadMissingCSS) != null ? ref2 : true,
        originalPath: message.originalPath || '',
        overrideURL: message.overrideURL || '',
        serverURL: "http://" + this.options.host + ":" + this.options.port
      });
    };

    LiveReload.prototype.performAlert = function(message) {
      return alert(message.message);
    };

    LiveReload.prototype.shutDown = function() {
      var base;
      if (!this.initialized) {
        return;
      }
      this.connector.disconnect();
      this.log("LiveReload disconnected.");
      return typeof (base = this.listeners).shutdown === "function" ? base.shutdown() : void 0;
    };

    LiveReload.prototype.hasPlugin = function(identifier) {
      return !!this.pluginIdentifiers[identifier];
    };

    LiveReload.prototype.addPlugin = function(pluginClass) {
      var plugin;
      if (!this.initialized) {
        return;
      }
      if (this.hasPlugin(pluginClass.identifier)) {
        return;
      }
      this.pluginIdentifiers[pluginClass.identifier] = true;
      plugin = new pluginClass(this.window, {
        _livereload: this,
        _reloader: this.reloader,
        _connector: this.connector,
        console: this.console,
        Timer: Timer,
        generateCacheBustUrl: (function(_this) {
          return function(url) {
            return _this.reloader.generateCacheBustUrl(url);
          };
        })(this)
      });
      this.plugins.push(plugin);
      this.reloader.addPlugin(plugin);
    };

    LiveReload.prototype.analyze = function() {
      var i, len, plugin, pluginData, pluginsData, ref;
      if (!this.initialized) {
        return;
      }
      if (!(this.connector.protocol >= 7)) {
        return;
      }
      pluginsData = {};
      ref = this.plugins;
      for (i = 0, len = ref.length; i < len; i++) {
        plugin = ref[i];
        pluginsData[plugin.constructor.identifier] = pluginData = (typeof plugin.analyze === "function" ? plugin.analyze() : void 0) || {};
        pluginData.version = plugin.constructor.version;
      }
      this.connector.sendCommand({
        command: 'info',
        plugins: pluginsData,
        url: this.window.location.href
      });
    };

    return LiveReload;

  })();

}).call(this);

},{"./connector":1,"./options":5,"./protocol":6,"./reloader":7,"./timer":9}],5:[function(require,module,exports){
(function() {
  var Options;

  exports.Options = Options = (function() {
    function Options() {
      this.https = false;
      this.host = null;
      this.port = 35729;
      this.snipver = null;
      this.ext = null;
      this.extver = null;
      this.mindelay = 1000;
      this.maxdelay = 60000;
      this.handshake_timeout = 5000;
    }

    Options.prototype.set = function(name, value) {
      if (typeof value === 'undefined') {
        return;
      }
      if (!isNaN(+value)) {
        value = +value;
      }
      return this[name] = value;
    };

    return Options;

  })();

  Options.extract = function(document) {
    var element, i, j, keyAndValue, len, len1, m, mm, options, pair, ref, ref1, src;
    ref = document.getElementsByTagName('script');
    for (i = 0, len = ref.length; i < len; i++) {
      element = ref[i];
      if ((src = element.src) && (m = src.match(/^[^:]+:\/\/(.*)\/z?livereload\.js(?:\?(.*))?$/))) {
        options = new Options();
        options.https = src.indexOf("https") === 0;
        if (mm = m[1].match(/^([^\/:]+)(?::(\d+))?(\/+.*)?$/)) {
          options.host = mm[1];
          if (mm[2]) {
            options.port = parseInt(mm[2], 10);
          }
        }
        if (m[2]) {
          ref1 = m[2].split('&');
          for (j = 0, len1 = ref1.length; j < len1; j++) {
            pair = ref1[j];
            if ((keyAndValue = pair.split('=')).length > 1) {
              options.set(keyAndValue[0].replace(/-/g, '_'), keyAndValue.slice(1).join('='));
            }
          }
        }
        return options;
      }
    }
    return null;
  };

}).call(this);

},{}],6:[function(require,module,exports){
(function() {
  var PROTOCOL_6, PROTOCOL_7, Parser, ProtocolError,
    indexOf = [].indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; };

  exports.PROTOCOL_6 = PROTOCOL_6 = 'http://livereload.com/protocols/official-6';

  exports.PROTOCOL_7 = PROTOCOL_7 = 'http://livereload.com/protocols/official-7';

  exports.ProtocolError = ProtocolError = (function() {
    function ProtocolError(reason, data) {
      this.message = "LiveReload protocol error (" + reason + ") after receiving data: \"" + data + "\".";
    }

    return ProtocolError;

  })();

  exports.Parser = Parser = (function() {
    function Parser(handlers) {
      this.handlers = handlers;
      this.reset();
    }

    Parser.prototype.reset = function() {
      return this.protocol = null;
    };

    Parser.prototype.process = function(data) {
      var command, e, error, message, options, ref;
      try {
        if (this.protocol == null) {
          if (data.match(/^!!ver:([\d.]+)$/)) {
            this.protocol = 6;
          } else if (message = this._parseMessage(data, ['hello'])) {
            if (!message.protocols.length) {
              throw new ProtocolError("no protocols specified in handshake message");
            } else if (indexOf.call(message.protocols, PROTOCOL_7) >= 0) {
              this.protocol = 7;
            } else if (indexOf.call(message.protocols, PROTOCOL_6) >= 0) {
              this.protocol = 6;
            } else {
              throw new ProtocolError("no supported protocols found");
            }
          }
          return this.handlers.connected(this.protocol);
        } else if (this.protocol === 6) {
          message = JSON.parse(data);
          if (!message.length) {
            throw new ProtocolError("protocol 6 messages must be arrays");
          }
          command = message[0], options = message[1];
          if (command !== 'refresh') {
            throw new ProtocolError("unknown protocol 6 command");
          }
          return this.handlers.message({
            command: 'reload',
            path: options.path,
            liveCSS: (ref = options.apply_css_live) != null ? ref : true
          });
        } else {
          message = this._parseMessage(data, ['reload', 'alert']);
          return this.handlers.message(message);
        }
      } catch (error) {
        e = error;
        if (e instanceof ProtocolError) {
          return this.handlers.error(e);
        } else {
          throw e;
        }
      }
    };

    Parser.prototype._parseMessage = function(data, validCommands) {
      var e, error, message, ref;
      try {
        message = JSON.parse(data);
      } catch (error) {
        e = error;
        throw new ProtocolError('unparsable JSON', data);
      }
      if (!message.command) {
        throw new ProtocolError('missing "command" key', data);
      }
      if (ref = message.command, indexOf.call(validCommands, ref) < 0) {
        throw new ProtocolError("invalid command '" + message.command + "', only valid commands are: " + (validCommands.join(', ')) + ")", data);
      }
      return message;
    };

    return Parser;

  })();

}).call(this);

},{}],7:[function(require,module,exports){
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

},{}],8:[function(require,module,exports){
(function() {
  var CustomEvents, LiveReload, k;

  CustomEvents = require('./customevents');

  LiveReload = window.LiveReload = new (require('./livereload').LiveReload)(window);

  for (k in window) {
    if (k.match(/^LiveReloadPlugin/)) {
      LiveReload.addPlugin(window[k]);
    }
  }

  LiveReload.addPlugin(require('./less'));

  LiveReload.on('shutdown', function() {
    return delete window.LiveReload;
  });

  LiveReload.on('connect', function() {
    return CustomEvents.fire(document, 'LiveReloadConnect');
  });

  LiveReload.on('disconnect', function() {
    return CustomEvents.fire(document, 'LiveReloadDisconnect');
  });

  CustomEvents.bind(document, 'LiveReloadShutDown', function() {
    return LiveReload.shutDown();
  });

}).call(this);

},{"./customevents":2,"./less":3,"./livereload":4}],9:[function(require,module,exports){
(function() {
  var Timer;

  exports.Timer = Timer = (function() {
    function Timer(func1) {
      this.func = func1;
      this.running = false;
      this.id = null;
      this._handler = (function(_this) {
        return function() {
          _this.running = false;
          _this.id = null;
          return _this.func();
        };
      })(this);
    }

    Timer.prototype.start = function(timeout) {
      if (this.running) {
        clearTimeout(this.id);
      }
      this.id = setTimeout(this._handler, timeout);
      return this.running = true;
    };

    Timer.prototype.stop = function() {
      if (this.running) {
        clearTimeout(this.id);
        this.running = false;
        return this.id = null;
      }
    };

    return Timer;

  })();

  Timer.start = function(timeout, func) {
    return setTimeout(func, timeout);
  };

}).call(this);

},{}]},{},[8]);
