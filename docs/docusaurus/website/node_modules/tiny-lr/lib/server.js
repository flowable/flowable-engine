import fs      from 'fs';
import http    from 'http';
import https   from 'https';
import events  from 'events';
import {parse} from 'url';
import Client  from './client';
import config  from '../package.json';
import anybody from 'body/any';
import qs      from 'qs';

const debug = require('debug')('tinylr:server');

const CONTENT_TYPE = 'content-type';
const FORM_TYPE = 'application/x-www-form-urlencoded';

function buildRootPath (prefix = '/') {
  let rootUrl = prefix;

  // Add trailing slash
  if (prefix[prefix.length - 1] !== '/') {
    rootUrl = `${rootUrl}/`;
  }

  // Add leading slash
  if (prefix[0] !== '/') {
    rootUrl = `/${rootUrl}`;
  }

  return rootUrl;
}

class Server extends events.EventEmitter {
  constructor (options = {}) {
    super();

    this.options = options;

    options.livereload = options.livereload || require.resolve('livereload-js/dist/livereload.js');

    // todo: change falsy check to allow 0 for random port
    options.port = parseInt(options.port || 35729, 10);

    if (options.errorListener) {
      this.errorListener = options.errorListener;
    }

    this.rootPath = buildRootPath(options.prefix);

    this.clients = {};
    this.configure(options.app);
    this.routes(options.app);
  }

  routes () {
    if (!this.options.dashboard) {
      this.on(`GET ${this.rootPath}`, this.index.bind(this));
    }

    this.on(`GET ${this.rootPath}changed`, this.changed.bind(this));
    this.on(`POST ${this.rootPath}changed`, this.changed.bind(this));
    this.on(`POST ${this.rootPath}alert`, this.alert.bind(this));
    this.on(`GET ${this.rootPath}livereload.js`, this.livereload.bind(this));
    this.on(`GET ${this.rootPath}kill`, this.close.bind(this));
  }

  configure (app) {
    debug('Configuring %s', app ? 'connect / express application' : 'HTTP server');

    let handler = this.options.handler || this.handler;

    if (!app) {
      if ((this.options.key && this.options.cert) || this.options.pfx) {
        this.server = https.createServer(this.options, handler.bind(this));
      } else {
        this.server = http.createServer(handler.bind(this));
      }

      this.server.on('upgrade', this.websocketify.bind(this));
      this.server.on('error', this.error.bind(this));
      return this;
    }

    this.app = app;
    this.app.listen = (port, done) => {
      done = done || function () {};
      if (port !== this.options.port) {
        debug('Warn: LiveReload port is not standard (%d). You are listening on %d', this.options.port, port);
        debug('You\'ll need to rely on the LiveReload snippet');
        debug('> http://feedback.livereload.com/knowledgebase/articles/86180-how-do-i-add-the-script-tag-manually-');
      }

      let srv = this.server = http.createServer(app);
      srv.on('upgrade', this.websocketify.bind(this));
      srv.on('error', this.error.bind(this));
      srv.on('close', this.close.bind(this));
      return srv.listen(port, done);
    };

    return this;
  }

  handler (req, res, next) {
    let middleware = typeof next === 'function';
    debug('LiveReload handler %s (middleware: %s)', req.url, middleware ? 'on' : 'off');

    next = next || this.defaultHandler.bind(this, res);
    req.headers[CONTENT_TYPE] = req.headers[CONTENT_TYPE] || FORM_TYPE;
    return anybody(req, res, (err, body) => {
      if (err) return next(err);
      req.body = body;

      if (!req.query) {
        req.query = req.url.indexOf('?') !== -1
          ? qs.parse(parse(req.url).query)
          : {};
      }

      return this.handle(req, res, next);
    });
  }

  index (req, res) {
    res.setHeader('Content-Type', 'application/json');
    res.write(JSON.stringify({
      tinylr: 'Welcome',
      version: config.version
    }));

    res.end();
  }

  handle (req, res, next) {
    let url = parse(req.url);
    debug('Request:', req.method, url.href);
    let middleware = typeof next === 'function';

    // do the routing
    let route = req.method + ' ' + url.pathname;
    let respond = this.emit(route, req, res);
    if (respond) return;

    if (middleware) return next();

    // Only apply content-type on non middleware setup #70
    return this.notFound(res);
  }

  defaultHandler (res, err) {
    if (!err) return this.notFound(res);

    this.error(err);
    res.setHeader('Content-Type', 'text/plain');
    res.statusCode = 500;
    res.end('Error: ' + err.stack);
  }

  notFound (res) {
    res.setHeader('Content-Type', 'application/json');
    res.writeHead(404);
    res.write(JSON.stringify({
      error: 'not_found',
      reason: 'no such route'
    }));
    res.end();
  }

  websocketify (req, socket, head) {
    let client = new Client(req, socket, head, this.options);
    this.clients[client.id] = client;

    // handle socket error to prevent possible app crash, such as ECONNRESET
    socket.on('error', (e) => {
      // ignore frequent ECONNRESET error (seems inevitable when refresh)
      if (e.code === 'ECONNRESET') return;
      this.error(e);
    });

    client.once('info', (data) => {
      debug('Create client %s (url: %s)', data.id, data.url);
      this.emit('MSG /create', data.id, data.url);
    });

    client.once('end', () => {
      debug('Destroy client %s (url: %s)', client.id, client.url);
      this.emit('MSG /destroy', client.id, client.url);
      delete this.clients[client.id];
    });
  }

  listen (port, host, fn) {
    port = port || this.options.port;

    // Last used port for error display
    this.port = port;

    if (typeof host === 'function') {
      fn = host;
      host = undefined;
    }

    this.server.listen(port, host, fn);
  }

  close (req, res) {
    Object.keys(this.clients).forEach(function (id) {
      this.clients[id].close();
    }, this);

    if (this.server._handle) this.server.close(this.emit.bind(this, 'close'));

    if (res) res.end();
  }

  error (e) {
    if (this.errorListener) {
      this.errorListener(e);
      return;
    }

    console.error();
    if (typeof e === 'undefined') {
      console.error('... Uhoh. Got error %s ...', e);
    } else {
      console.error('... Uhoh. Got error %s ...', e.message);
      console.error(e.stack);

      if (e.code !== 'EADDRINUSE') return;
      console.error();
      console.error('You already have a server listening on %s', this.port);
      console.error('You should stop it and try again.');
      console.error();
    }
  }

  // Routes

  livereload (req, res) {
    res.setHeader('Content-Type', 'application/javascript');
    fs.createReadStream(this.options.livereload).pipe(res);
  }

  changed (req, res) {
    let files = this.param('files', req);

    debug('Changed event (Files: %s)', files.join(' '));
    let clients = this.notifyClients(files);

    if (!res) return;

    res.setHeader('Content-Type', 'application/json');
    res.write(JSON.stringify({
      clients: clients,
      files: files
    }));

    res.end();
  }

  alert (req, res) {
    let message = this.param('message', req);

    debug('Alert event (Message: %s)', message);
    let clients = this.alertClients(message);

    if (!res) return;

    res.setHeader('Content-Type', 'application/json');
    res.write(JSON.stringify({
      clients: clients,
      message: message
    }));

    res.end();
  }

  notifyClients (files) {
    let clients = Object.keys(this.clients).map(function (id) {
      let client = this.clients[id];
      debug('Reloading client %s (url: %s)', client.id, client.url);
      client.reload(files);
      return {
        id: client.id,
        url: client.url
      };
    }, this);

    return clients;
  };

  alertClients (message) {
    let clients = Object.keys(this.clients).map(function (id) {
      let client = this.clients[id];
      debug('Alert client %s (url: %s)', client.id, client.url);
      client.alert(message);
      return {
        id: client.id,
        url: client.url
      };
    }, this);

    return clients;
  }

  // Lookup param from body / params / query.
  param (name, req) {
    let param;
    if (req.body && req.body[name]) param = req.body[name];
    else if (req.params && req.params[name]) param = req.params[name];
    else if (req.query && req.query[name]) param = req.query[name];

    // normalize files array
    if (name === 'files') {
      param = Array.isArray(param) ? param
        : typeof param === 'string' ? param.split(/[\s,]/)
        : [];
    }

    return param;
  }
}

export default Server;
