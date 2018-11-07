# Flowable Slate UI

## Introduction

Utility project to build a static web site for the Public REST API

## Tooling

We're using the [widdershins](https://github.com/mermade/widdershins) library to translate swagger definitions into slate compatible markdown. This needs to be installed on your system too: 

```ruby 
npm install [-g] widdershins
```

We're using the **sed** GNU Linux command. This needs to be installed on your system too (if not present): 

On Mac Os 
```ruby 
brew install gnu-sed --with-default-names
```

## Usage

Execute **generate.sh** to generate slate website based on Swagger (v2) API Specification.
or
Execute **generate-oas.sh** to generate slate website based on OpenAPI API Specification.

We recommend to use **generate-oas.sh**.

For each API a website will be available in **target/slate** folder.

Drop the folder into a web server or open index.html directly in your web browser.

