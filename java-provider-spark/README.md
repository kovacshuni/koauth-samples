# KOAuth Sample for Java

Example application for how to use the [KOAuth](https://github.com/kovacshuni/koauth) library in Java.

## Quick:

```
git clone https://github.com/kovacshuni/koauth-sample-java.git
cd koauth-sample-java
mvn clean install
mvn exec:java
```

Go to [http://127.0.0.1:4567/requestToken](http://127.0.0.1:4567/requestToken) and after a series of steps you
should see your latest tweet as a result.

## How it works:

I chose the widely popular [Twitter API](https://dev.twitter.com/rest/public)

1. Requests a request token from the Twitter API
2. Redirects you to Twitter's authorization page, where you have to autorize your key.
3. Redirects you back to this app, exchanging your authorized request token to an access token.
4. Redirects you to this apps endpoint which, using your new acces key, retrieves and displays your last tweet.

I'm using [koauth-sync](https://github.com/kovacshuni/koauth-sync) rather than koauth, it's better suited for Java.
Java 1.8 required.

## Contributing

Just create a pull-request, we'll discuss it, i'll try to be quick.

## Owner

Hunor Kovács  
kovacshuni@yahoo.com  
[hunorkovacs.com](http://www.hunorkovacs.com)

## Licence

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0) .
