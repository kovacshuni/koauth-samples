package com.hunorkovacs.koauth.sample.java.providerspark;

import com.hunorkovacs.koauth.domain.*;
import com.hunorkovacs.koauthsync.domain.mapper.ResponseMapper;
import com.hunorkovacs.koauthsync.domain.mapper.RequestMapper;
import com.hunorkovacs.koauthsync.service.provider.ProviderService;
import com.hunorkovacs.koauthsync.service.provider.ProviderServiceFactory;
import com.hunorkovacs.koauth.service.DefaultTokenGenerator;

import scala.Option;
import scala.concurrent.ExecutionContext;
import scala.util.Either;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.servlet.http.HttpServletResponse;

import static spark.Spark.*;

public class OauthServlet {

    public static final String CONSUMER_KEY = "OmFjJKNqU4v791CWj6QKaBaiEep0WBxJ";

    private ExecutionContext ec = ExecutionContext.Implicits$.MODULE$.global();
    private MyExampleMemoryPersistence persistence = new MyExampleMemoryPersistence(ec);
    private ProviderService provider = ProviderServiceFactory.createProviderService(persistence, ec);
    private RequestMapper<Request> requestMapper = new SparkRequestMapper();

    private class RequestTokenRoute implements Route {
        public Object handle(Request request, spark.Response response) throws Exception {
            KoauthRequest koauthRequest = requestMapper.map(request);
            KoauthResponse koauthResponse = provider.requestToken(koauthRequest);
            return new SparkResponseMapper(response).map(koauthResponse);
        }
    }

    private class AuthorizeRoute implements Route {
        public Object handle(Request request, spark.Response response) throws Exception {
            String username = request.params("username");
            if (username.equals("admin") && request.params("password").equals("admin")) {
                String requestToken = request.params("requestToken");
                String verifier = DefaultTokenGenerator.generateVerifier();
                persistence.authorizeRequestToken(requestToken, username, verifier);
                Option<String> callbackUrl = persistence.getCallback(CONSUMER_KEY, requestToken);
                response.redirect(callbackUrl.get() + "?" + OauthParams.VerifierName() + "=" + verifier);
            } else {
                response.status(401);
                response.body("Credentials or token not valid.");
            }
            return response;
        }
    }

    private class AccessTokenRoute implements Route {
        public Object handle(Request request, spark.Response response) throws Exception {
            KoauthRequest koauthRequest = requestMapper.map(request);
            KoauthResponse koauthResponse = provider.accessToken(koauthRequest);
            return new SparkResponseMapper(response).map(koauthResponse);
        }
    }

    private class MeRoute implements Route {
        public Object handle(Request request, Response response) throws Exception {
            KoauthRequest koauthRequest = requestMapper.map(request);
            Either<KoauthResponse, String> authentication = provider.oauthenticate(koauthRequest);
            if (authentication.isLeft()) {
                KoauthResponse left = authentication.left().get();
                if (left.getClass().equals(ResponseUnauthorized.class)) {
                    response.status(401);
                    return "You are treated as a guest.\n" + ((ResponseUnauthorized) left).body();
                } else {
                    response.status(400);
                    return "You are treated as a guest.\n" + ((ResponseBadRequest) left).body();
                }
            } else {
                String username = authentication.right().get();
                return "You are " + username + ".";
            }
        }
    }

    public static void main(String[] args) {
        OauthServlet twitter = new OauthServlet();
        post("/oauth/request-token", twitter.new RequestTokenRoute());
        get("/oauth/authorize-with-password", twitter.new AuthorizeRoute());
        post("/oauth/access-token", twitter.new AccessTokenRoute());
        get("/me", twitter.new MeRoute());
    }

    private class SparkRequestMapper implements RequestMapper<Request> {
        public KoauthRequest map(Request source) {
            Option<String> body;
            if (source.contentLength() > 0) {
                body = Option.apply(source.body());
            } else {
                body = Option.empty();
            }
            return KoauthRequest.apply(source.requestMethod(), source.url(),
                    Option.apply(source.headers("Authorization")), body);
        }
    }

    private class SparkResponseMapper implements ResponseMapper<spark.Response> {

        private Response response;

        private SparkResponseMapper(Response response) {
            this.response = response;
        }

        public spark.Response map(KoauthResponse source) {
            if (source.getClass().equals(ResponseOk.class)) {
                response.body(((ResponseOk) source).body());
            } else {
                if (source.getClass().equals(ResponseUnauthorized.class)) {
                    response.status(401);
                    response.body(((ResponseUnauthorized) source).body());
                } else {
                    response.status(400);
                    response.body(((ResponseBadRequest) source).body());
                }
            }
            return response;
        }
    }
}
