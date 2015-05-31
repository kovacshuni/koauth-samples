# KOAuth Sample Consumer in Java

Example application for how to use the [KOAuth](https://github.com/kovacshuni/koauth) library as OAuth consumer in Java.
Authenticates you in Twitter and displays your latest tweet.

## Quick:

```
git clone https://github.com/kovacshuni/koauth-samples.git
cd koauth-samples
mvn clean install
mvn exec:java
```

Go to [http://127.0.0.1:4567/requestToken](http://127.0.0.1:4567/requestToken) and after a series of steps you
should see your latest tweet as a result.

## How it works:

1. Requests a request token from the Twitter API
2. Redirects you to Twitter's authorization page, where you have to autorize your key.
3. Redirects you back to this app, exchanging your authorized request token to an access token.
4. Redirects you to this apps endpoint which, using your new acces key, retrieves and displays your last tweet.

[Twitter API](https://dev.twitter.com/rest/public)

I'm using [koauth-sync](https://github.com/kovacshuni/koauth-sync) rather than koauth. It's just a wrapper that
simplifies every API call, giving back results directly without Futures. It's better suited for Java and newbies
who don't know [Akka](https://akka.io) yet.
