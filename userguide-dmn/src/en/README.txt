Tooling
-------

Install Asciidoctor: http://asciidoctor.org/

We're using the 'pygments' library for syntax highlighting. This needs to be installed on your system too: gem install pygments.rb


Generating the docs
--------------------

Call the ./generate-all.sh script to generate both the html and pdf docs in the 'output' folder.
Use ./generate-html.sh for html only and likewise ./generate-pdf.sh for pdf only.


Docs on the docs
----------------

The html is generated using the 'index-html.adoc' file. The pdf generation uses the 'index-pdf.adoc' file. Both reference a shared 'index-common.adoc' file for the actual content, but define different parameters in the preamble.

When building the html doc, following files get included automatically:

* flowable.css : this is the stylesheet for the docs. The css will be included inline in the html docs.
* docinfo.html : this file gets included at the top of the html file. It contains the tocbot library and a little script to initialize the dynamic table of contents.
