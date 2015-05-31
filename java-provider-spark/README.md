# KOAuth Sample Provider in Java

Example application for how to use the [KOAuth](https://github.com/kovacshuni/koauth) library as OAuth provider in Java.
Creates a servlet. You can create/obtain tokens and, using them, make a protected API call.

## Quick:

```
git clone https://github.com/kovacshuni/koauth-samples.git
cd koauth-samples
mvn clean install
mvn exec:java
```

## How to use

1. Request a new Request Token with an HTTP POST to http://127.0.0.1:4567/request-token
2. Auhtorize your requested token with HTTP GET to http://127.0.0.1:4567/authorize-with-password?username=admin&password=admin&requestToken=yourrequesttokenobtainedatstep1
3. Exchange your authorized request token to an access token by making an HTTP POST to http://127.0.0.1:4567/access-token
4. Make a protected API call using your Access Token by making an HTTP GET to http://127.0.0.1:4567/me

As a result you should see the text: 'You are admin.'

If your don't sign it correctly or don't sign at all, you will be shown 'You are treated as a guest.', and an error message.

I'm using [koauth-sync](https://github.com/kovacshuni/koauth-sync) rather than koauth. It's just a wrapper that
simplifies every API call, giving back results directly without Futures. It's better suited for Java and newbies
who don't know [Akka](https://akka.io) yet.
