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
