import React from 'react';
import Layout from '@theme/Layout';

const links = [
  ['BPMN', 'https://flowable.com/open-source/docs/javadocs/index.html'],
  ['CMMN', 'https://flowable.com/open-source/docs/cmmn-javadocs/index.html'],
  ['Task', 'https://flowable.com/open-source/docs/task-javadocs/index.html'],
  ['Variable', 'https://flowable.com/open-source/docs/variable-javadocs/index.html'],
  ['Job', 'https://flowable.com/open-source/docs/job-javadocs/index.html'],
  ['Batch', 'https://flowable.com/open-source/docs/batch-javadocs/index.html'],
  ['Entity Link', 'https://flowable.com/open-source/docs/entitylink-javadocs/index.html'],
  ['Event Registry', 'https://flowable.com/open-source/docs/eventregistry-javadocs/index.html'],
  ['Event Subscription', 'https://flowable.com/open-source/docs/eventsubscription-javadocs/index.html'],
];

export default function Javadocs() {
  return (
    <Layout title="Flowable Javadocs" description="Java Developer Documentation">
      <main className="flowableHome">
        <section>
          <h1>Flowable Javadocs</h1>
          <p>Java developer reference documentation for Flowable modules.</p>
          <div className="flowableLinkRow">
            {links.map(([label, href]) => (
              <a className="button button--primary" href={href} target="_blank" rel="noreferrer" key={label}>{label}</a>
            ))}
          </div>
        </section>
      </main>
    </Layout>
  );
}
