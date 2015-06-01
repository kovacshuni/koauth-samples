# KOAuth Sample Consumer in Scala

Example application for how to use the [KOAuth](https://github.com/kovacshuni/koauth) library as OAuth consumer in Scala.
Authenticates you in Twitter and displays your latest tweet.

## Quick:

```
git clone https://github.com/kovacshuni/koauth-samples.git
cd koauth-samples/scala-consumer-scalatra
sbt
container:start
```

Go to [http://127.0.0.1:4567/requestToken](http://127.0.0.1:4567/requestToken) and after a series of steps you
should see your latest tweet as a result.

## How it works:

1. Requests a request token from the Twitter API
2. Redirects you to Twitter's authorization page, where you have to autorize your key.
3. Redirects you back to this app, exchanging your authorized request token to an access token.
4. Redirects you to this apps endpoint which, using your new acces key, retrieves and displays your last tweet.

[Twitter API](https://dev.twitter.com/rest/public)
