/**
 * Copyright (c) 2017-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

const React = require('react');

class Footer extends React.Component {
  docUrl(doc, language) {
    const baseUrl = this.props.config.baseUrl;
    const docsUrl = this.props.config.docsUrl;
    const docsPart = `${docsUrl ? `${docsUrl}/` : ''}`;
    const langPart = `${language ? `${language}/` : ''}`;
    return `${baseUrl}${docsPart}${langPart}${doc}`;
  }

  pageUrl(doc, language) {
    const baseUrl = this.props.config.baseUrl;
    return baseUrl + (language ? `${language}/` : '') + doc;
  }

  render() {
    return (
      <footer className="nav-footer" id="footer">
        <section className="sitemap">
          <a href={this.props.config.baseUrl} className="nav-home">
            {this.props.config.footerIcon && (
              <img
                src={this.props.config.baseUrl + this.props.config.footerIcon}
                alt={this.props.config.title}
              />
            )}
          </a>
          <div>
            <h5>Documentation</h5>
            <a href={this.docUrl('oss-introduction', this.props.language)}>
              Guides
            </a>
            <a href={this.docUrl('javadocs', this.props.language)}>
              Javadocs
            </a>
          </div>
          <div>
            <h5>Disclaimer</h5>
            <p className="disclaimer">Flowable may change, amend or delete information contained in its documents at any time and without formal arrangement. Documents and other informational items concerning a release of a Flowable product that has not yet been officially released by Flowable are non-binding and may be incomplete or contain errors. Flowable strives to provide complete and exact information in its documents. If permitted by applicable law, Flowable takes no responsibility for the accuracy of the issued informational items and documentation and does not provide any warranty for their content. Flowable assumes no liability for any direct, indirect or incidental damages incurred by the use of its documents and informational items. Documents or informational items do not change or amend the contractual terms and conditions regarding Flowable products.</p>
          </div>
        </section>

        <section className="copyright">{this.props.config.copyright}</section>
      </footer>
    );
  }
}

module.exports = Footer;
