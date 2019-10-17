import events       from 'events';
import WebSocket    from 'faye-websocket';
import objectAssign from 'object-assign';

const debug = require('debug')('tinylr:client');

let idCounter = 0;

export default class Client extends events.EventEmitter {

  constructor (req, socket, head, options = {}) {
    super();
    this.options = options;
    this.ws = new WebSocket(req, socket, head);
    this.ws.onmessage = this.message.bind(this);
    this.ws.onclose = this.close.bind(this);
    this.id = this.uniqueId('ws');
  }

  message (event) {
    let data = this.data(event);
    if (this[data.command]) return this[data.command](data);
  }

  close (event) {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }

    this.emit('end', event);
  }

  // Commands
  hello () {
    this.send({
      command: 'hello',
      protocols: [
        'http://livereload.com/protocols/official-7'
      ],
      serverName: 'tiny-lr'
    });
  }

  info (data) {
    if (data) {
      debug('Info', data);
      this.emit('info', objectAssign({}, data, { id: this.id }));
      this.plugins = data.plugins;
      this.url = data.url;
    }

    return objectAssign({}, data || {}, { id: this.id, url: this.url });
  }

  // Server commands
  reload (files) {
    files.forEach(function (file) {
      this.send({
        command: 'reload',
        path: file,
        liveCSS: this.options.liveCSS !== false,
        reloadMissingCSS: this.options.reloadMissingCSS !== false,
        liveImg: this.options.liveImg !== false
      });
    }, this);
  }

  alert (message) {
    this.send({
      command: 'alert',
      message: message
    });
  }

  // Utilities
  data (event) {
    let data = {};
    try {
      data = JSON.parse(event.data);
    } catch (e) {}
    return data;
  }

  send (data) {
    if (!this.ws) return;
    this.ws.send(JSON.stringify(data));
  }

  uniqueId (prefix) {
    let id = idCounter++;
    return prefix ? prefix + id : id;
  }
}
