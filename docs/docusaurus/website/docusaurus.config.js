const {themes} = require('prism-react-renderer');

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Flowable Open Source Documentation',
  tagline: 'Reference and User Guides',
  favicon: 'img/favicon.png',
  url: 'https://flowable.com',
  baseUrl: '/open-source/docs/',
  organizationName: 'flowable',
  projectName: 'flowable-userguide',
  onBrokenLinks: 'warn',
  markdown: {
    format: 'md',
    hooks: {
      onBrokenMarkdownLinks: 'warn',
      onBrokenMarkdownImages: 'warn',
    },
  },

  presets: [
    [
      'classic',
      {
        docs: {
          path: '../docs',
          routeBasePath: '/',
          sidebarPath: require.resolve('./sidebars.js'),
        },
        blog: false,
        theme: {
          customCss: require.resolve('./static/css/custom.css'),
        },
      },
    ],
  ],

  themeConfig: {
    navbar: {
      title: 'Flowable Open Source Documentation',
      logo: {
        alt: 'Flowable',
        src: 'img/flowable-oss-icon@2.png',
      },
      items: [
        {to: '/oss-introduction', label: 'Guides', position: 'left'},
        {to: '/all-javadocs', label: 'Javadocs', position: 'left'},
        {href: 'https://flowable.com/open-source/', label: 'Open source home', position: 'right'},
      ],
    },
    footer: {
      style: 'dark',
      copyright: `Copyright © ${new Date().getFullYear()} Flowable AG`,
    },
    colorMode: {
      defaultMode: 'light',
      disableSwitch: true,
    },
    prism: {
      theme: themes.github,
      darkTheme: themes.dracula,
    },
  },
};

module.exports = config;
