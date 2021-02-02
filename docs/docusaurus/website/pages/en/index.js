/**
 * Copyright (c) 2017-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

const React = require('react');

const CompLibrary = require('../../core/CompLibrary.js');

const MarkdownBlock = CompLibrary.MarkdownBlock; /* Used to read markdown */
const Container = CompLibrary.Container;
const GridBlock = CompLibrary.GridBlock;

class Index extends React.Component {
  render() {
    const {config: siteConfig, language = ''} = this.props;
    const {baseUrl, docsUrl} = siteConfig;
    const docsPart = `${docsUrl ? `${docsUrl}/` : ''}`;
    const langPart = `${language ? `${language}/` : ''}`;
    const docUrl = doc => `${baseUrl}${docsPart}${langPart}${doc}`;

    const Block = props => (
      <Container
        padding={['bottom', 'top']}
        id={props.id}
        background={props.background}>
        <GridBlock
          align="left"
          contents={props.children}
          layout={props.layout}
        />
      </Container>
    );

    const PromoSection = props => (
      <div className="section promoSection">
        <div className="promoRow">
          <div className="pluginRowBlock">{props.children}</div>
        </div>
      </div>
    );

    const Button = props => (
      <div className="pluginWrapper buttonWrapper">
        <a className="button" href={props.href} target={props.target}>
          {props.children}
        </a>
      </div>
    );

    const Talks = () => (
<Container
  padding={['bottom', 'top']}
  background="light"
  className="productShowcaseSection">
        <h2>Talks and tutorials</h2>
<div className="productShowcaseSection">
<p>If you just want to download Flowable and have a quick play, this is a good article to follow:</p>
<a className="normal-link" href="https://blog.flowable.org/2020/10/07/flowable-6-instant-gratification/">Instant Gratification with Flowable 6</a>

<h3>Here are some talks we’ve given:</h3>
<a  className="normal-link" href="https://www.youtube.com/watch?v=uSQVtm8O7SA">bpmNEXT 2019: The Case of the Intentional Process</a><br/>
<a  className="normal-link" href="https://www.youtube.com/watch?v=nX0dRiPqOmk">Devoxx 2019: Flowable business processing from Kafka events</a><br/>
<a  className="normal-link" href="https://www.youtube.com/watch?v=fyxRHZaCSSA">bpmNEXT 2018: Making Process Personal</a><br/>
<a  className="normal-link" href="https://www.youtube.com/watch?v=vzgU1lZ1h3U">Jax London 2018: Using transactional BPM for service orchestration on NoSQL</a><br/>
<a  className="normal-link" href="https://www.youtube.com/watch?v=i8dYR0LdpHg">Devoxx 2017: Introducing AI to the Flowable Process Engines</a><br/>
<a  className="normal-link" href="https://www.youtube.com/watch?v=5qIw3JTw-mI">bpmNEXT 2017: Making Business Processes Dance to the User’s Tune</a><br/>
<a  className="normal-link" href="https://www.youtube.com/watch?v=HaZQTrfNAgo">Codemotion 2017: Introducing AI to the Flowable Process Engines</a><br/>

<h3>FlowFest Community Event tasks:</h3>
<a  className="normal-link" href="https://flowable.com/flowfest2018">FlowFest 2018: All talks</a><br/>
<a  className="normal-link" href="https://flowable.com/flowfest2019">FlowFest 2019: All talks</a>

<h3>And links to some others:</h3>
<a  className="normal-link" href="https://www.youtube.com/watch?v=43_OLrxU3so">Josh Long: Spring Tips: Business Process Management with Flowable</a>
</div>
      </Container>
    );

    const Started = () => (
<Container
  padding={['bottom', 'top']}
  className="productShowcaseSection">
        <h2>Getting started</h2>
        <MarkdownBlock>This is the best place to start if you’re new to Flowable. It shows you how to get up and running in minutes, either with the UI Apps or introducing you to the basics of the Engine API.</MarkdownBlock>
          <PromoSection>
            <Button href={docUrl('bpmn/ch14-Applications')}>Getting started with Apps</Button>
            <Button href={docUrl('bpmn/ch02-GettingStarted')}>Getting started with Code</Button>
          </PromoSection>
      </Container>
    );

    const Guides = () => (
<Container
  padding={['bottom', 'top']}
  background="light"
  className="productShowcaseSection">
          <h2>Guides</h2>
        <MarkdownBlock>The User Guides contain a detailed explanation of the different Flowable engine features. BPMN for process management, CMMN for case management and DMN for decision rules.</MarkdownBlock>
          <PromoSection>
            <Button href={docUrl('bpmn/ch02-GettingStarted')}>BPMN</Button>
            <Button href={docUrl('cmmn/ch02-Configuration')}>CMMN</Button>
            <Button href={docUrl('eventregistry/ch02-Configuration')}>Event Registry</Button>
            <Button href={docUrl('dmn/ch02-Configuration')}>DMN</Button>
            <Button href={docUrl('form/ch02-Configuration')}>Form</Button>
          </PromoSection>
      </Container>
    );

    const REST = () => (
<Container
  padding={['bottom', 'top']}
  className="productShowcaseSection">
        <h2>REST APIs</h2>
        <MarkdownBlock>Flowable has rich REST APIs that provides almost full coverage of the Flowable Java APIs. These are the easiest and most flexible APIs to work with.
In addition we’ve added Swagger in Flowable v6. Boot up the Flowable REST app to try it out by accessing http://localhost:8080/flowable-rest/docs</MarkdownBlock>
          <PromoSection>
            <Button href={docUrl('bpmn/ch15-REST')}>BPMN</Button>
            <Button href={docUrl('cmmn/ch08-REST')}>CMMN</Button>
            <Button href={docUrl('dmn/ch07-REST')}>DMN</Button>
            <Button href={docUrl('eventregistry/ch07-REST')}>Event Registry</Button>
            <Button href={docUrl('form/ch07-REST')}>Form</Button>
          </PromoSection>
      </Container>
    );

    const Head = () => (
<Container
  padding={['bottom', 'top']}>
        <GridBlock
          align="left"
          contents={[
          {
      title: 'Flowable Open Source Documentation',
      content: 'Welcome to the Flowable Open Source Documentation. Here you can find guides and reference documents to help you develop applications and services that use Flowable BPM. This documentation is under constant update, so be sure to visit here regularly',
      image: `${baseUrl}img/flowable-boffin.png`,
          },
          ]}
        />
      </Container>
    );

    const Flowable5 = () => (
      <Block background="light">
        {[
          {
            content:
              'The migration guide provides information on how to upgrade Flowable or Activiti v5 to Flowable v6. There are no database changes needed and for most people just renames of packages and classes.<br/><br/>[View the Migration Guide](migration)',
            image: `${baseUrl}img/flowable-beta.png`,
            imageAlign: 'left',
            title: 'Flowable 5 Migration',
          },
        ]}
      </Block>
    );

    const Olddocs = () => (
<Container
  padding={['bottom', 'top']}
  className="productShowcaseSection">
        <h2>Flowable 5 Documentation</h2>
        <MarkdownBlock>The Flowable 5 version of the documentation items above can be found on these pages.</MarkdownBlock>
          <PromoSection>
            <Button href="https://flowable.com/open-source/docs/userguide-5/index.html#_getting_started">Getting Started v5</Button>
            <Button href="https://flowable.com/open-source/docs/userguide-5/index.html">User Guide v5</Button>
            <Button href="https://flowable.com/open-source/docs/userguide-5/index.html#restApiChapter">REST API v5</Button>
            <Button href="https://flowable.com/open-source/docs/javadocs-5/index.html">Javadocs v5</Button>
          </PromoSection>
      </Container>
    );

    return (
      <div>
          <Head />
        <div className="mainContainer">
          <Talks />
          <Started />
          <Guides />
          <REST />
          <Flowable5 />
          <Olddocs />
        </div>
      </div>
    );
  }
}

module.exports = Index;
