import xml from 'xml'

const GENERATOR = 'Feed for Node.js'
const DOCTYPE = '<?xml version="1.0" encoding="utf-8"?>\n'

class Feed {

  constructor(options) {
    this.options = options
    this.items = []
    this.categories = []
    this.contributors = []
    this.extensions = []
  }

  addItem(item) {
    this.items.push(item)
  }

  addCategory(category) {
    this.categories.push(category)
  }

  addContributor(contributor) {
    this.contributors.push(contributor)
  }

  addExtension(extension) {
    this.extensions.push(extension)
  }

  render(format) {
    console.warn('DEPRECATED: use atom1() or rss2() instead of render()');
    if (format === 'atom-1.0') {
      return this.atom1();
    } else {
      return this.rss2();
    }
  }

  atom1() {
    const { options } = this

    let feed = [
      { _attr: { xmlns: 'http://www.w3.org/2005/Atom' } },
      { id: options.id },
      { title: options.title },
      { updated: (options.updated ? this.ISODateString(options.updated) : this.ISODateString(new Date())) },
      { generator: options.generator || GENERATOR },
    ]

    let root = [{ feed }]

    if (options.author) {
      const { name, email, link } = options.author
      let author = []

      if (name) {
        author.push({ name });
      }

      if (email) {
        author.push({ email });
      }

      if (link) {
        author.push({ uri: link });
      }

      feed.push({ author })
    }

    // link (rel="alternate")
    if(options.link) {
      feed.push({ link: { _attr: { rel: 'alternate', href: options.link }}});
    }

    // link (rel="self")
    const atomLink = options.feed || (options.feedLinks && options.feedLinks.atom);
    if(atomLink) {
      feed.push({ "link": { _attr: { rel: 'self', href: atomLink }}});
    }

    // link (rel="hub")
    if(options.hub) {
      feed.push({ link: { _attr: { rel:'hub', href: options.hub }}});
    }

    /**************************************************************************
     * "feed" node: optional elements
     *************************************************************************/

    if(options.description) {
      feed.push({ subtitle: options.description });
    }

    if(options.image) {
      feed.push({ logo: options.image });
    }

    if(options.favicon) {
      feed.push({ icon: options.favicon });
    }

    if(options.copyright) {
      feed.push({ rights: options.copyright });
    }

    this.categories.forEach(category => {
      feed.push({ category: [{ _attr: { term: category } }] });
    })

    this.contributors.forEach(item => {
      const { name, email, link } = item
      let contributor = [];

      if(name) {
        contributor.push({ name });
      }

      if(email) {
        contributor.push({ email });
      }

      if(link) {
        contributor.push({ uri: link });
      }

      feed.push({ contributor });
    })

    // icon

    /**************************************************************************
     * "entry" nodes
     *************************************************************************/
    this.items.forEach(item => {
      //
      // entry: required elements
      //

      let entry = [
        { title: { _attr: { type: 'html' }, _cdata: item.title }},
        { id: item.id || item.link },
        { link: [{ _attr: { href: item.link } }]},
        { updated: this.ISODateString(item.date) }
      ]

      //
      // entry: recommended elements
      //
      if(item.description) {
        entry.push({ summary: { _attr: { type: 'html' }, _cdata: item.description }});
      }

      if(item.content) {
        entry.push({ content: { _attr: { type: 'html' }, _cdata: item.content }});
      }

      // entry author(s)
      if(Array.isArray(item.author)) {
        item.author.forEach(oneAuthor => {
          const { name, email, link } = oneAuthor
          let author = [];

          if(name) {
            author.push({ name });
          }

          if(email) {
            author.push({ email });
          }

          if(link) {
            author.push({ uri: link });
          }

          entry.push({ author });
        })
      }

      // content

      // link - relative link to article

      //
      // entry: optional elements
      //

      // category

      // contributor
      if(Array.isArray(item.contributor)) {
        item.contributor.forEach(item => {
          const { name, email, link } = item
          let contributor = [];

          if(name) {
            contributor.push({ name });
          }

          if(email) {
            contributor.push({ email });
          }

          if(link) {
            contributor.push({ uri: link });
          }

          entry.push({ contributor });
        })
      }

      // published
      if(item.published) {
        entry.push({ published: this.ISODateString(item.published) });
      }

      // source

      // rights
      if(item.copyright) {
        entry.push({ rights: item.copyright });
      }

      feed.push({ entry: entry });
    })

    return DOCTYPE + xml(root, true);
  }

  rss2() {
    const { options } = this
    let isAtom = false
    let isContent = false

    let channel = [
      { title: options.title },
      { link: options.link },
      { description: options.description },
      { lastBuildDate: (options.updated ? options.updated.toUTCString() : new Date().toUTCString()) },
      { docs: 'http://blogs.law.harvard.edu/tech/rss'},
      { generator: options.generator || GENERATOR },
    ]

    let rss = [
      { _attr: { version: '2.0' } },
      { channel },
    ]

    let root = [{ rss }]

    /**
     * Channel Image
     * http://cyber.law.harvard.edu/rss/rss.html#ltimagegtSubelementOfLtchannelgt
     */
    if(options.image) {
       channel.push({
        image: [
          { title: options.title },
          { url: options.image },
          { link: options.link },
        ]
      });
     }

    /**
     * Channel Copyright
     * http://cyber.law.harvard.edu/rss/rss.html#optionalChannelElements
     */
    if(options.copyright) {
      channel.push({ copyright: options.copyright });
    }

    /**
     * Channel Categories
     * http://cyber.law.harvard.edu/rss/rss.html#comments
     */
    this.categories.forEach(category => {
      channel.push({ category });
    })

    /**
     * Feed URL
     * http://validator.w3.org/feed/docs/warning/MissingAtomSelfLink.html
     */
    const atomLink = options.feed || (options.feedLinks && options.feedLinks.atom);
    if(atomLink) {
      isAtom = true

      channel.push({
        "atom:link": {
          _attr: {
            href: atomLink,
            rel: 'self',
            type: 'application/rss+xml',
          },
        },
      })
    }

    /**
     * Hub for PubSubHubbub
     * https://code.google.com/p/pubsubhubbub/
     */
    if(options.hub) {
      isAtom = true;
      channel.push({
        "atom:link": {
          _attr: {
            href: options.hub,
            rel: 'hub',
          },
        },
      })
    }

    /**
     * Channel Categories
     * http://cyber.law.harvard.edu/rss/rss.html#hrelementsOfLtitemgt
     */
    this.items.forEach(entry => {
      let item = [];

      if(entry.title) {
        item.push({ title: { _cdata: entry.title }});
      }

      if(entry.link) {
        item.push({ link: entry.link });
      }

      if(entry.guid) {
        item.push({ guid: entry.guid });
      } else if (entry.link) {
        item.push({ guid: entry.link });
      }

      if(entry.date) {
        item.push({ pubDate: entry.date.toUTCString() });
      }

      if(entry.description) {
        item.push({ description: { _cdata: entry.description }});
      }

      if(entry.content) {
        isContent = true;
        item.push({ 'content:encoded': { _cdata: entry.content }});
      }
      /**
       * Item Author
       * http://cyber.law.harvard.edu/rss/rss.html#ltauthorgtSubelementOfLtitemgt
       */
      if(Array.isArray(entry.author)) {
        entry.author.some(author => {
          if (author.email && author.name) {
            item.push({ author: author.email + ' (' + author.name + ')' })
            return true
          } else {
            return false
          }
        })
      }

      if(entry.image) {
        item.push({ enclosure: [{ _attr: { url: entry.image } }] });
      }

      channel.push({ item });
    })

    if(isContent) {
      rss[0]._attr['xmlns:content'] = 'http://purl.org/rss/1.0/modules/content/';
    }

    if(isAtom) {
      rss[0]._attr['xmlns:atom'] = 'http://www.w3.org/2005/Atom';
    }

    return DOCTYPE + xml(root, true);
  }

  json1() {
    const { options, items, extensions } = this
    let feed = {
      version: 'https://jsonfeed.org/version/1',
      title: options.title,
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

    extensions.forEach(e => {
      feed[e.name] = e.objects;
    });

    feed.items = items.map(item => {
      let feedItem = {
        id: item.id,
        // json_feed distinguishes between html and text content
        // but since we only take a single type, we'll assume HTML
        html_content: item.content,
      }
      if (item.link) {
        feedItem.url = item.link;
      }
      if(item.title) {
        feedItem.title = item.title;
      }
      if (item.description) {
        feedItem.summary = item.description;
      }

      if (item.image) {
        feedItem.image = item.image
      }

      if (item.date) {
        feedItem.date_modified = this.ISODateString(item.date);
      }
      if (item.published) {
        feedItem.date_published = this.ISODateString(item.published);
      }

      if (item.author) {
        let author = item.author;
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

      if(item.extensions) {
        item.extensions.forEach(e => {
          feedItem[e.name] = e.objects;
        });
      }

      return feedItem;
    });

    return JSON.stringify(feed, null, 4);
  }

  ISODateString(d) {
    function pad(n) {
      return n<10 ? '0'+n : n
    }

    return d.getUTCFullYear() + '-'
      + pad(d.getUTCMonth() + 1) + '-'
      + pad(d.getUTCDate()) + 'T'
      + pad(d.getUTCHours()) + ':'
      + pad(d.getUTCMinutes()) + ':'
      + pad(d.getUTCSeconds()) + 'Z'
  }

}

module.exports = Feed
