'use strict';

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _xml = require('xml');

var _xml2 = _interopRequireDefault(_xml);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

var GENERATOR = 'Feed for Node.js';
var DOCTYPE = '<?xml version="1.0" encoding="utf-8"?>\n';

var Feed = function () {
  function Feed(options) {
    _classCallCheck(this, Feed);

    this.options = options;
    this.items = [];
    this.categories = [];
    this.contributors = [];
    this.extensions = [];
  }

  _createClass(Feed, [{
    key: 'addItem',
    value: function addItem(item) {
      this.items.push(item);
    }
  }, {
    key: 'addCategory',
    value: function addCategory(category) {
      this.categories.push(category);
    }
  }, {
    key: 'addContributor',
    value: function addContributor(contributor) {
      this.contributors.push(contributor);
    }
  }, {
    key: 'addExtension',
    value: function addExtension(extension) {
      this.extensions.push(extension);
    }
  }, {
    key: 'render',
    value: function render(format) {
      console.warn('DEPRECATED: use atom1() or rss2() instead of render()');
      if (format === 'atom-1.0') {
        return this.atom1();
      } else {
        return this.rss2();
      }
    }
  }, {
    key: 'atom1',
    value: function atom1() {
      var _this = this;

      var options = this.options;


      var feed = [{ _attr: { xmlns: 'http://www.w3.org/2005/Atom' } }, { id: options.id }, { title: options.title }, { updated: options.updated ? this.ISODateString(options.updated) : this.ISODateString(new Date()) }, { generator: options.generator || GENERATOR }];

      var root = [{ feed: feed }];

      if (options.author) {
        var _options$author = options.author,
            name = _options$author.name,
            email = _options$author.email,
            link = _options$author.link;

        var author = [];

        if (name) {
          author.push({ name: name });
        }

        if (email) {
          author.push({ email: email });
        }

        if (link) {
          author.push({ uri: link });
        }

        feed.push({ author: author });
      }

      // link (rel="alternate")
      if (options.link) {
        feed.push({ link: { _attr: { rel: 'alternate', href: options.link } } });
      }

      // link (rel="self")
      var atomLink = options.feed || options.feedLinks && options.feedLinks.atom;
      if (atomLink) {
        feed.push({ "link": { _attr: { rel: 'self', href: atomLink } } });
      }

      // link (rel="hub")
      if (options.hub) {
        feed.push({ link: { _attr: { rel: 'hub', href: options.hub } } });
      }

      /**************************************************************************
       * "feed" node: optional elements
       *************************************************************************/

      if (options.description) {
        feed.push({ subtitle: options.description });
      }

      if (options.image) {
        feed.push({ logo: options.image });
      }

      if (options.favicon) {
        feed.push({ icon: options.favicon });
      }

      if (options.copyright) {
        feed.push({ rights: options.copyright });
      }

      this.categories.forEach(function (category) {
        feed.push({ category: [{ _attr: { term: category } }] });
      });

      this.contributors.forEach(function (item) {
        var name = item.name,
            email = item.email,
            link = item.link;

        var contributor = [];

        if (name) {
          contributor.push({ name: name });
        }

        if (email) {
          contributor.push({ email: email });
        }

        if (link) {
          contributor.push({ uri: link });
        }

        feed.push({ contributor: contributor });
      });

      // icon

      /**************************************************************************
       * "entry" nodes
       *************************************************************************/
      this.items.forEach(function (item) {
        //
        // entry: required elements
        //

        var entry = [{ title: { _attr: { type: 'html' }, _cdata: item.title } }, { id: item.id || item.link }, { link: [{ _attr: { href: item.link } }] }, { updated: _this.ISODateString(item.date) }];

        //
        // entry: recommended elements
        //
        if (item.description) {
          entry.push({ summary: { _attr: { type: 'html' }, _cdata: item.description } });
        }

        if (item.content) {
          entry.push({ content: { _attr: { type: 'html' }, _cdata: item.content } });
        }

        // entry author(s)
        if (Array.isArray(item.author)) {
          item.author.forEach(function (oneAuthor) {
            var name = oneAuthor.name,
                email = oneAuthor.email,
                link = oneAuthor.link;

            var author = [];

            if (name) {
              author.push({ name: name });
            }

            if (email) {
              author.push({ email: email });
            }

            if (link) {
              author.push({ uri: link });
            }

            entry.push({ author: author });
          });
        }

        // content

        // link - relative link to article

        //
        // entry: optional elements
        //

        // category

        // contributor
        if (Array.isArray(item.contributor)) {
          item.contributor.forEach(function (item) {
            var name = item.name,
                email = item.email,
                link = item.link;

            var contributor = [];

            if (name) {
              contributor.push({ name: name });
            }

            if (email) {
              contributor.push({ email: email });
            }

            if (link) {
              contributor.push({ uri: link });
            }

            entry.push({ contributor: contributor });
          });
        }

        // published
        if (item.published) {
          entry.push({ published: _this.ISODateString(item.published) });
        }

        // source

        // rights
        if (item.copyright) {
          entry.push({ rights: item.copyright });
        }

        feed.push({ entry: entry });
      });

      return DOCTYPE + (0, _xml2.default)(root, true);
    }
  }, {
    key: 'rss2',
    value: function rss2() {
      var options = this.options;

      var isAtom = false;
      var isContent = false;

      var channel = [{ title: options.title }, { link: options.link }, { description: options.description }, { lastBuildDate: options.updated ? options.updated.toUTCString() : new Date().toUTCString() }, { docs: 'http://blogs.law.harvard.edu/tech/rss' }, { generator: options.generator || GENERATOR }];

      var rss = [{ _attr: { version: '2.0' } }, { channel: channel }];

      var root = [{ rss: rss }];

      /**
       * Channel Image
       * http://cyber.law.harvard.edu/rss/rss.html#ltimagegtSubelementOfLtchannelgt
       */
      if (options.image) {
        channel.push({
          image: [{ title: options.title }, { url: options.image }, { link: options.link }]
        });
      }

      /**
       * Channel Copyright
       * http://cyber.law.harvard.edu/rss/rss.html#optionalChannelElements
       */
      if (options.copyright) {
        channel.push({ copyright: options.copyright });
      }

      /**
       * Channel Categories
       * http://cyber.law.harvard.edu/rss/rss.html#comments
       */
      this.categories.forEach(function (category) {
        channel.push({ category: category });
      });

      /**
       * Feed URL
       * http://validator.w3.org/feed/docs/warning/MissingAtomSelfLink.html
       */
      var atomLink = options.feed || options.feedLinks && options.feedLinks.atom;
      if (atomLink) {
        isAtom = true;

        channel.push({
          "atom:link": {
            _attr: {
              href: atomLink,
              rel: 'self',
              type: 'application/rss+xml'
            }
          }
        });
      }

      /**
       * Hub for PubSubHubbub
       * https://code.google.com/p/pubsubhubbub/
       */
      if (options.hub) {
        isAtom = true;
        channel.push({
          "atom:link": {
            _attr: {
              href: options.hub,
              rel: 'hub'
            }
          }
        });
      }

      /**
       * Channel Categories
       * http://cyber.law.harvard.edu/rss/rss.html#hrelementsOfLtitemgt
       */
      this.items.forEach(function (entry) {
        var item = [];

        if (entry.title) {
          item.push({ title: { _cdata: entry.title } });
        }

        if (entry.link) {
          item.push({ link: entry.link });
        }

        if (entry.guid) {
          item.push({ guid: entry.guid });
        } else if (entry.link) {
          item.push({ guid: entry.link });
        }

        if (entry.date) {
          item.push({ pubDate: entry.date.toUTCString() });
        }

        if (entry.description) {
          item.push({ description: { _cdata: entry.description } });
        }

        if (entry.content) {
          isContent = true;
          item.push({ 'content:encoded': { _cdata: entry.content } });
        }
        /**
         * Item Author
         * http://cyber.law.harvard.edu/rss/rss.html#ltauthorgtSubelementOfLtitemgt
         */
        if (Array.isArray(entry.author)) {
          entry.author.some(function (author) {
            if (author.email && author.name) {
              item.push({ author: author.email + ' (' + author.name + ')' });
              return true;
            } else {
              return false;
            }
          });
        }

        if (entry.image) {
          item.push({ enclosure: [{ _attr: { url: entry.image } }] });
        }

        channel.push({ item: item });
      });

      if (isContent) {
        rss[0]._attr['xmlns:content'] = 'http://purl.org/rss/1.0/modules/content/';
      }

      if (isAtom) {
        rss[0]._attr['xmlns:atom'] = 'http://www.w3.org/2005/Atom';
      }

      return DOCTYPE + (0, _xml2.default)(root, true);
    }
  }, {
    key: 'json1',
    value: function json1() {
      var _this2 = this;

      var options = this.options,
          items = this.items,
          extensions = this.extensions;

      var feed = {
        version: 'https://jsonfeed.org/version/1',
        title: options.title
      };

      if (options.link) {
        feed.home_page_url = options.link;
      }

      if (options.feedLinks && options.feedLinks.json) {
        feed.feed_url = options.feedLinks.json;
      }

      if (options.description) {
        feed.description = options.description;
      }

      if (options.image) {
        feed.icon = options.image;
      }

      if (options.author) {
        feed.author = {};
        if (options.author.name) {
          feed.author.name = options.author.name;
        }
        if (options.author.link) {
          feed.author.url = options.author.link;
        }
      }

      extensions.forEach(function (e) {
        feed[e.name] = e.objects;
      });

      feed.items = items.map(function (item) {
        var feedItem = {
          id: item.id,
          // json_feed distinguishes between html and text content
          // but since we only take a single type, we'll assume HTML
          html_content: item.content
        };
        if (item.link) {
          feedItem.url = item.link;
        }
        if (item.title) {
          feedItem.title = item.title;
        }
        if (item.description) {
          feedItem.summary = item.description;
        }

        if (item.image) {
          feedItem.image = item.image;
        }

        if (item.date) {
          feedItem.date_modified = _this2.ISODateString(item.date);
        }
        if (item.published) {
          feedItem.date_published = _this2.ISODateString(item.published);
        }

        if (item.author) {
          var author = item.author;
          if (author instanceof Array) {
            // json feed only supports 1 author per post
            author = author[0];
          }
          feedItem.author = {};
          if (author.name) {
            feedItem.author.name = author.name;
          }
          if (author.link) {
            feedItem.author.url = author.link;
          }
        }

        if (item.extensions) {
          item.extensions.forEach(function (e) {
            feedItem[e.name] = e.objects;
          });
        }

        return feedItem;
      });

      return JSON.stringify(feed, null, 4);
    }
  }, {
    key: 'ISODateString',
    value: function ISODateString(d) {
      function pad(n) {
        return n < 10 ? '0' + n : n;
      }

      return d.getUTCFullYear() + '-' + pad(d.getUTCMonth() + 1) + '-' + pad(d.getUTCDate()) + 'T' + pad(d.getUTCHours()) + ':' + pad(d.getUTCMinutes()) + ':' + pad(d.getUTCSeconds()) + 'Z';
    }
  }]);

  return Feed;
}();

module.exports = Feed;
//# sourceMappingURL=feed.js.map