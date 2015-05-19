package com.hunorkovacs.koauthsamplejava;

import com.hunorkovacs.koauth.domain.OauthParams;
import com.hunorkovacs.koauth.domain.TokenResponse;
import com.hunorkovacs.koauth.domain.KoauthRequest;
import com.hunorkovacs.koauthsync.service.consumer.ConsumerService;
import com.hunorkovacs.koauthsync.service.consumer.DefaultConsumerService;
import com.hunorkovacs.koauth.service.consumer.RequestWithInfo;
import com.hunorkovacs.koauth.service.Arithmetics;

import scala.Option;
import scala.concurrent.ExecutionContext;
import spark.Request;
import spark.Route;

import javax.ws.rs.client.*;
import javax.ws.rs.core.Response;

import static spark.Spark.*;

public class Twitter {

    private static final String CONSUMER_KEY = "E0XkGScbAci70CeBikSYh90EF";
    private static final String CONSUMER_SECRET = "3BIeeX56BuRng0Yq7MJCxaT15wH5YgJ1QmFkfGKw7yFPZbE3Eh";
    private static final String REQUEST_TOKEN_URL = "https://api.twitter.com/oauth/request_token";
    private static final String AUTHORIZE_TOKEN_URL = "https://api.twitter.com/oauth/authorize";
    private static final String ACCESS_TOKEN_URL = "https://api.twitter.com/oauth/access_token";

    private ExecutionContext ec = ExecutionContext.Implicits$.MODULE$.global();
    private Client http = ClientBuilder.newClient();
    private ConsumerService consumer = new DefaultConsumerService(ec);

    private TokenResponse requestTokenResponse;
    private TokenResponse accessTokenResponse;

    private class RequestTokenRoute implements Route {
        public Object handle(Request request, spark.Response response) throws Exception {
            requestTokenResponse = obtainRequestToken();
            response.redirect(AUTHORIZE_TOKEN_URL +
                    "?" + OauthParams.TokenName() + "=" + requestTokenResponse.token());
            return response;
        }
    }

    private class AccessTokenRoute implements Route {
        public Object handle(Request request, spark.Response response) throws Exception {
            accessTokenResponse = exchangeForAccessToken(requestTokenResponse.token(),
                    requestTokenResponse.secret(),
                    request.queryMap(OauthParams.VerifierName()).value());
            response.redirect("/lastTweet");
            return response;
        }
    }

    private class LastTweetRoute implements Route {
        public Object handle(Request request, spark.Response response) throws Exception {
            return obtainLastTweet(accessTokenResponse.token(), accessTokenResponse.secret());
        }
    }

    public static void main(String[] args) {
        Twitter twitter = new Twitter();
        get("/requestToken", twitter.new RequestTokenRoute());
        get("/accessToken", twitter.new AccessTokenRoute());
        get("/lastTweet", twitter.new LastTweetRoute());
    }

    private TokenResponse obtainRequestToken() {
        Invocation.Builder builder = http.target(REQUEST_TOKEN_URL).request();
        RequestWithInfo requestWithInfo = consumer.createRequestTokenRequest(KoauthRequest.apply("POST",
                        REQUEST_TOKEN_URL, Option.<String>empty()),
                CONSUMER_KEY,
                CONSUMER_SECRET,
                "http://127.0.0.1:4567/accessToken");
        Invocation invocation = builder.header("Authorization", requestWithInfo.header()).buildPost(Entity.text(""));

        Response response = invocation.invoke();

        System.out.println("Response: HTTP " + response.getStatus());
        String body = response.readEntity(String.class);
        System.out.println(body);
        return Arithmetics.parseRequestTokenResponse(body).right().get();
    }

    private TokenResponse exchangeForAccessToken(String requestToken, String requestTokenSecret, String verifier) {
        Invocation.Builder builder = http.target(ACCESS_TOKEN_URL).request();
        RequestWithInfo requestWithInfo = consumer.createAccessTokenRequest(KoauthRequest.apply("POST",
                        ACCESS_TOKEN_URL, Option.<String>empty()),
                CONSUMER_KEY,
                CONSUMER_SECRET,
                requestToken,
                requestTokenSecret,
                verifier);
        Invocation invocation = builder.header("Authorization", requestWithInfo.header()).buildPost(Entity.text(""));

        Response response = invocation.invoke();

        System.out.println("Response: HTTP " + response.getStatus());
        String body = response.readEntity(String.class);
        System.out.println(body);
        return Arithmetics.parseAccessTokenResponse(body).right().get();
    }

    private String obtainLastTweet(String accessToken, String accessTokenSecret) {
        String lastTweetUrl = "https://api.twitter.com/1.1/statuses/user_timeline.json?count=1&include_rts=1&trim_user=true";
        Invocation.Builder builder = http.target(lastTweetUrl).request();
        RequestWithInfo requestWithInfo = consumer.createOauthenticatedRequest(KoauthRequest.apply("GET",
                        lastTweetUrl, Option.<String>empty()),
                CONSUMER_KEY,
                CONSUMER_SECRET,
                accessToken,
                accessTokenSecret);
        Invocation invocation = builder.header("Authorization", requestWithInfo.header()).buildGet();

        Response response = invocation.invoke();

        System.out.println("Response: HTTP " + response.getStatus());
        String body = response.readEntity(String.class);
        System.out.println(body);
        return body;
    }
}
