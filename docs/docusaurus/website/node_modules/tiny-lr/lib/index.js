import Server from './server';
import Client from './client';

const debug = require('debug')('tinylr');

// Need to keep track of LR servers when notifying
const servers = [];

export default tinylr;

// Expose Server / Client objects
tinylr.Server = Server;
tinylr.Client = Client;

// and the middleware helpers
tinylr.middleware = middleware;
tinylr.changed = changed;

// Main entry point
function tinylr (opts) {
  const srv = new Server(opts);
  servers.push(srv);
  return srv;
}

// A facade to Server#handle
function middleware (opts) {
  const srv = new Server(opts);
  servers.push(srv);
  return function tinylr (req, res, next) {
    srv.handler(req, res, next);
  };
}

// Changed helper, helps with notifying the server of a file change
function changed (done) {
  const files = [].slice.call(arguments);
  if (typeof files[files.length - 1] === 'function') done = files.pop();
  done = typeof done === 'function' ? done : () => {};
  debug('Notifying %d servers - Files: ', servers.length, files);
  servers.forEach(srv => {
    const params = { params: { files: files } };
    srv && srv.changed(params);
  });
  done();
}
