# Flowable Slate UI

## Introduction

Utility project to build a static web site for the Public REST API

## Tooling

We're using the [widdershins](https://github.com/mermade/widdershins) library to translate swagger defintions into slate compatible markdown. This needs to be installed on your system too: 

```ruby 
npm install [-g] widdershins
```

## Usage

Execute **generate.sh**

For each API a website will be available in **target/slate** folder.

Drop the folder into a web server or open index.html directly in your web browser.

