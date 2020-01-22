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

class HomeSplash extends React.Component {
  render() {
    const {siteConfig, language = ''} = this.props;
    const {baseUrl, docsUrl} = siteConfig;
    const docsPart = `${docsUrl ? `${docsUrl}/` : ''}`;
    const langPart = `${language ? `${language}/` : ''}`;
    const docUrl = doc => `${baseUrl}${docsPart}${langPart}${doc}`;

    const SplashContainer = props => (
      <div className="homeContainer">
        <div className="homeSplashFade">
          <div className="wrapper homeWrapper">{props.children}</div>
        </div>
      </div>
    );

    const Logo = props => (
      <div className="projectLogo">
        <img src={props.img_src} alt="Project Logo" />
      </div>
    );

    const ProjectTitle = () => (
      <h2 className="projectTitle">
        Flowable Javadocs
        <small>Java Developer Documentation</small>
      </h2>
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

    return (
      <SplashContainer>
        <div className="inner">
          <ProjectTitle />
<p>For the tightest integration to your applications, you can use the Java API.  From high-level to the deepest customization, this is the definitive reference document.</p>
          <PromoSection>
            <Button href={'https://flowable.com/open-source/docs/javadocs/index.html'} target="_blank">BPMN</Button>
            <Button href={'https://flowable.com/open-source/docs/cmmn-javadocs/index.html'} target="_blank">CMMN</Button>
            <Button href={'https://flowable.com/open-source/docs/task-javadocs/index.html'} target="_blank">Task</Button>
          </PromoSection>
          <PromoSection>
            <Button href={'https://flowable.com/open-source/docs/variable-javadocs/index.html'} target="_blank">Variable</Button>
            <Button href={'https://flowable.com/open-source/docs/job-javadocs/index.html'} target="_blank">Job</Button>
            <Button href={'https://flowable.com/open-source/docs/batch-javadocs/index.html'} target="_blank">Batch</Button>
            <Button href={'https://flowable.com/open-source/docs/entitylink-javadocs/index.html'} target="_blank">Entity Link</Button>
          </PromoSection>
          <PromoSection>
            <Button href={'https://flowable.com/open-source/docs/eventregistry-javadocs/index.html'} target="_blank">Event Registry</Button>
            <Button href={'https://flowable.com/open-source/docs/eventsubscription-javadocs/index.html'} target="_blank">Event Subscription</Button>
          </PromoSection>
        </div>
      </SplashContainer>
    );
  }
}

class Javadocs extends React.Component {
  render() {
    const {config: siteConfig, language = ''} = this.props;
    const {baseUrl} = siteConfig;

    const Block = props => (
      <Container
        padding={['bottom', 'top']}
        id={props.id}
        background={props.background}>
        <GridBlock
          align="center"
          contents={props.children}
          layout={props.layout}
        />
      </Container>
    );

    return (
      <div>
        <HomeSplash siteConfig={siteConfig} language={language} />
      </div>
    );
  }
}

module.exports = Javadocs;
