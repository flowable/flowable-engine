Steps to upgrade:

* Remove content of docs folder
* Download latest release from https://github.com/swagger-api/swagger-ui
* Unzip + copy dist folder contents to docs folder
* Edit index.html and update javascript to contain "url = "specfile/flowable.json";"
* Edit index.html to change to swagger-ui.min.js instead of non-minified version.