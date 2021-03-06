# KOAuth Sample Provider in Scala

Example application for how to use the [KOAuth](https://github.com/kovacshuni/koauth) library as OAuth provider in Scala.
Creates a servlet. You can create/obtain tokens and, using them, make a protected API call.

## Quick:

```
git clone https://github.com/kovacshuni/koauth-samples.git
cd koauth-samples/scala-provider-scalatra
sbt
container:start
```

## How to use

1. Request a new Request Token with an HTTP POST to http://127.0.0.1:8080/request-token
2. Auhtorize your requested token with HTTP GET to http://127.0.0.1:8080/authorize-with-password?username=admin&password=admin&requestToken=yourrequesttokenobtainedatstep1
3. Exchange your authorized Request Token to an Access Token by making an HTTP POST to http://127.0.0.1:8080/access-token
4. Make a protected API call using your Access Token, an HTTP GET to http://127.0.0.1:8080/me

As a result you should see the text: _You are admin._

If your don't sign it correctly or don't sign at all, you will be shown _You are treated as a guest._, and an error message.
