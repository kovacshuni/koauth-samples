package com.hunorkovacs.koauth.sample.java.providerspark;

import com.hunorkovacs.koauth.service.provider.persistence.ExampleMemoryPersistence;
import com.hunorkovacs.koauth.service.provider.persistence.RequestToken;
import com.hunorkovacs.koauthsync.service.provider.persistence.Persistence;
import com.hunorkovacs.koauthsync.service.provider.persistence.SyncWrapPersistence;
import scala.Option;
import scala.collection.Iterator;
import scala.collection.mutable.ListBuffer;
import scala.concurrent.ExecutionContext;

public class MyExampleMemoryPersistence implements Persistence {

    private ExampleMemoryPersistence exampleMemoryPers;
    private SyncWrapPersistence syncPers;

    public MyExampleMemoryPersistence(ExecutionContext ec) {
        exampleMemoryPers = new ExampleMemoryPersistence(ec);
        syncPers = new SyncWrapPersistence(exampleMemoryPers);
    }

    public void authorizeRequestToken(String requestToken, String username, String verifier) {
        ListBuffer<RequestToken> requestTokens = exampleMemoryPers.requestTokens();
        Iterator<RequestToken> tokenIterator = requestTokens.iterator();
        while (tokenIterator.hasNext()) {
            RequestToken t = tokenIterator.next();
            if (t.requestToken().equals(requestToken)) {
                requestTokens.remove(requestTokens.indexOf(t));
                requestTokens.$plus$eq(RequestToken.apply(OauthServlet.CONSUMER_KEY, t.requestToken(), t.requestTokenSecret(),
                        t.callback(), Option.apply(username), Option.apply(verifier)));
                return;
            }
        }
        throw new RuntimeException("Request Token didn't exist");
    }

    public boolean nonceExists(String nonce, String consumerKey, String token) {
        return syncPers.nonceExists(nonce, consumerKey, token);
    }

    public void persistNonce(String nonce, String consumerKey, String token) {
        syncPers.persistNonce(nonce, consumerKey, token);
    }

    public void persistRequestToken(String consumerKey, String requestToken, String requestTokenSecret, String callback) {
        syncPers.persistRequestToken(consumerKey, requestToken, requestTokenSecret, callback);
    }

    public Option<String> getConsumerSecret(String consumerKey) {
        return syncPers.getConsumerSecret(consumerKey);
    }

    public Option<String> whoAuthorizedRequestToken(String consumerKey, String requestToken, String verifier) {
        return syncPers.whoAuthorizedRequestToken(consumerKey, requestToken, verifier);
    }

    public Option<String> getCallback(String consumerKey, String requestToken) {
        return syncPers.getCallback(consumerKey, requestToken);
    }

    public void persistAccessToken(String consumerKey, String accessToken, String accessTokenSecret, String username) {
        syncPers.persistAccessToken(consumerKey, accessToken, accessTokenSecret, username);
    }

    public void deleteRequestToken(String consumerKey, String requestToken) {
        syncPers.deleteRequestToken(consumerKey, requestToken);
    }

    public Option<String> getRequestTokenSecret(String consumerKey, String requestToken) {
        return syncPers.getRequestTokenSecret(consumerKey, requestToken);
    }

    public Option<String> getAccessTokenSecret(String consumerKey, String accessToken) {
        return syncPers.getAccessTokenSecret(consumerKey, accessToken);
    }

    public Option<String> getUsername(String consumerKey, String accessToken) {
        return syncPers.getUsername(consumerKey, accessToken);
    }
}
