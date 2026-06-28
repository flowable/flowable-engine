import React from 'react';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';

const guideLinks = [
  ['BPMN', '/bpmn/ch02-GettingStarted'],
  ['CMMN', '/cmmn/ch02-Configuration'],
  ['Event Registry', '/eventregistry/ch02-Configuration'],
  ['DMN', '/dmn/ch02-Configuration'],
];

const restLinks = [
  ['BPMN', '/bpmn/ch14-REST'],
  ['CMMN', '/cmmn/ch08-REST'],
  ['DMN', '/dmn/ch07-REST'],
  ['Event Registry', '/eventregistry/ch07-REST'],
];

function LinkRow({links}) {
  return (
    <div className="flowableLinkRow">
      {links.map(([label, to]) => (
        <Link className="button button--primary" to={to} key={label}>{label}</Link>
      ))}
    </div>
  );
}

export default function Home() {
  const {siteConfig} = useDocusaurusContext();

  return (
    <Layout title={siteConfig.title} description={siteConfig.tagline}>
      <main className="flowableHome">
        <section className="flowableHero">
          <img src="img/flowable-boffin.png" alt="" />
          <div>
            <h1>Flowable Open Source Documentation</h1>
            <p>Guides and reference documents for applications and services that use Flowable BPM.</p>
          </div>
        </section>
        <section>
          <h2>Getting started</h2>
          <p>Start with the application guide or the core engine API examples.</p>
          <LinkRow links={[["Getting started with Apps", '/bpmn/ch13-Applications'], ["Getting started with Code", '/bpmn/ch02-GettingStarted']]} />
        </section>
        <section>
          <h2>Guides</h2>
          <p>User guides for BPMN, CMMN, DMN, Event Registry, and Forms.</p>
          <LinkRow links={guideLinks} />
        </section>
        <section>
          <h2>REST APIs</h2>
          <p>Reference material for the Flowable REST APIs.</p>
          <LinkRow links={restLinks} />
        </section>
      </main>
    </Layout>
  );
}
