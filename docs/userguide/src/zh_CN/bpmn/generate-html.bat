call asciidoctor -a stylesheet=../base/flowable.css -o output/index.html index-html.adoc
@echo on
call asciidoctor -a stylesheet=../base/flowable.css -o output/migration.html migration.adoc
@echo on

rd /s /q "output/images"
mkdir "output/images"
copy /y "images" "output/images"

copy /y "../base/images" "output/images/"