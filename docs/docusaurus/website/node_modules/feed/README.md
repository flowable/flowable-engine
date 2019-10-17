# Feed for Node.js

> [Feed](http://projets.jpmonette.net/en/feed) is a *RSS 2.0*, *JSON Feed 1.0*, and *Atom 1.0* generator for **Node.js**, making content syndication simple and intuitive!

[![Build Status](https://travis-ci.org/jpmonette/feed.svg?branch=master)](https://travis-ci.org/jpmonette/feed)
[![Coverage Status](https://coveralls.io/repos/github/jpmonette/feed/badge.svg?branch=master)](https://coveralls.io/github/jpmonette/feed?branch=master)

## Installation

```bash
$ npm install feed
```

## Features

* Pure JavaScript
* Support for Atom 1.0 and RSS 2.0
* Lightweight - Only 1 dependency!

## Quick Start

First, add the module:

```js
const Feed = require('feed')
```

Insert feed-specific information:

```js
let feed = new Feed({
  title: 'Feed Title',
  description: 'This is my personal feed!',
  id: 'http://example.com/',
  link: 'http://example.com/',
  image: 'http://example.com/image.png',
  favicon: 'http://example.com/favicon.ico',
  copyright: 'All rights reserved 2013, John Doe',
  updated: new Date(2013, 06, 14), // optional, default = today
  generator: 'awesome', // optional, default = 'Feed for Node.js'
  feedLinks: {
    json: 'https://example.com/json',
    atom: 'https://example.com/atom',
  },
  author: {
    name: 'John Doe',
    email: 'johndoe@example.com',
    link: 'https://example.com/johndoe'
  }
})
```

Insert items using the item function:

```js
posts.forEach(post => {
  feed.addItem({
    title: post.title,
    id: post.url,
    link: post.url,
    description: post.description,
    content: post.content,
    author: [{
      name: 'Jane Doe',
      email: 'janedoe@example.com',
      link: 'https://example.com/janedoe'
    }, {
      name: 'Joe Smith',
      email: 'joesmith@example.com',
      link: 'https://example.com/joesmith'
    }],
    contributor: [{
      name: 'Shawn Kemp',
      email: 'shawnkemp@example.com',
      link: 'https://example.com/shawnkemp'
    }, {
      name: 'Reggie Miller',
      email: 'reggiemiller@example.com',
      link: 'https://example.com/reggiemiller'
    }],
    date: post.date,
    image: post.image
  })
})
```

Insert categories using:

```js
feed.addCategory('Technologie')
```

Insert contributors using:

```js
feed.addContributor({
  name: 'Johan Cruyff',
  email: 'johancruyff@example.com',
  link: 'https://example.com/johancruyff'
})
```

Output a RSS 2.0 feed:

```js
feed.rss2()
```

Output an Atom 1.0 feed:

```js
feed.atom1()
```

Output a JSON Feed 1.0 feed:

```js
feed.json1()
```

Yes, it's that simple :)!

## More Information

* [Feed for Node.js](http://projets.jpmonette.net/en/feed) (English)
* [Feed pour Node.js](http://projets.jpmonette.net/feed) (French)
* Follow [@jpmonette](https://twitter.com/jpmonette) on Twitter for updates
* Read my personal blog [Blogue de Jean-Philippe Monette](http://blogue.jpmonette.net/) to learn more about what I do!

## License

Copyright (C) 2013, Jean-Philippe Monette <contact@jpmonette.net>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
