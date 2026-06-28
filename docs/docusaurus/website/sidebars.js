/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  docs: [
    {
      type: 'category',
      label: 'Flowable Open Source',
      items: ['oss-introduction'],
    },
    {
      type: 'category',
      label: 'BPMN User Guide',
      items: [
        'bpmn/ch02-GettingStarted',
        'bpmn/ch03-Configuration',
        'bpmn/ch04-API',
        'bpmn/ch05-Spring',
        'bpmn/ch05a-Spring-Boot',
        'bpmn/ch06-Deployment',
        'bpmn/ch07a-BPMN-Introduction',
        'bpmn/ch07b-BPMN-Constructs',
        'bpmn/ch08-ProcessInstanceMigration',
        'bpmn/ch09-JPA',
        'bpmn/ch10-History',
        'bpmn/ch11-IDM',
        'bpmn/ch14-REST',
        'bpmn/ch16-Cdi',
        'bpmn/ch16-Ldap',
        'bpmn/ch18-Advanced',
        'bpmn/ch18-tooling',
      ],
    },
    {
      type: 'category',
      label: 'CMMN User Guide',
      items: [
        'cmmn/ch02-Configuration',
        'cmmn/ch03-API',
        'cmmn/ch04-Spring',
        'cmmn/ch05-Deployment',
        'cmmn/ch06-cmmn',
        'cmmn/ch07-architecture',
        'cmmn/ch08-REST',
      ],
    },
    {
      type: 'category',
      label: 'Event Registry User Guide',
      items: [
        'eventregistry/ch02-Configuration',
        'eventregistry/ch03-API',
        'eventregistry/ch04-Spring',
        'eventregistry/ch05-Deployment',
        'eventregistry/ch06-EventRegistry-Introduction',
        'eventregistry/ch07-REST',
      ],
    },
    {
      type: 'category',
      label: 'DMN User Guide',
      items: [
        'dmn/ch02-Configuration',
        'dmn/ch03-API',
        'dmn/ch04-Spring',
        'dmn/ch05-Deployment',
        'dmn/ch06-DMN-Introduction',
        'dmn/ch07-REST',
      ],
    },
    {
      type: 'category',
      label: 'Applications Guide',
      items: ['bpmn/ch13-Applications', 'bpmn/ch12-Design'],
    },
    {
      type: 'category',
      label: 'Disclaimer',
      items: ['disclaimer'],
    },
  ],
};

module.exports = sidebars;
