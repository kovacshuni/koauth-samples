package com.hunorkovacs.koauth.sample.scala.consumerscalatra

import java.util.concurrent.ConcurrentHashMap

import _root_.akka.actor.ActorSystem
import com.hunorkovacs.koauth.domain.OauthParams._
import com.hunorkovacs.koauth.domain.{TokenResponse, KoauthRequest}
import com.hunorkovacs.koauth.service.Arithmetics._
import com.hunorkovacs.koauth.service.consumer.DefaultConsumerService
import org.scalatra._
import spray.client.pipelining
import spray.client.pipelining._
import spray.http.HttpHeaders.RawHeader
import spray.http._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class Twitter extends ScalatraServlet {

  private val ConsumerKey = "TZO4QQGaFJBjXuKsyh8n1KrpE"
  private val ConsumerSecret = "7t57qMb1unIyiZrRVuWwSwAQ6QXxcUtuRh98iqeHQSjfXtFsIx"
  private val RequestTokenUrl = "https://api.twitter.com/oauth/request_token"
  private val AuthorizeTokenUrl = "https://api.twitter.com/oauth/authorize"
  private val AccessTokenUrl = "https://api.twitter.com/oauth/access_token"
  /**
   * You don't have to deal with userId, neither adding it to your Callback URLs or any other,
   * it's just for this app, as i am multiplexing many users that are trying to authenticate with different keys.
   */
  private val UserId = "userId"

  implicit private val system = ActorSystem()
  import system.dispatcher
  private val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
  private val consumer = new DefaultConsumerService(system.dispatcher)
  private val tokens = new ConcurrentHashMap[Int, TokenResponse]()
  private val random = scala.util.Random

  get("/requestToken") {
    val userId = random.nextInt(1000)
    val requestTokenResponseF = consumer.createRequestTokenRequest(
        KoauthRequest("POST", RequestTokenUrl, None), ConsumerKey, ConsumerSecret,
          s"http://127.0.0.1:8080/accessToken?$UserId=$userId") flatMap { requestWithInfo =>
      pipeline(pipelining.Post(RequestTokenUrl).withHeaders(RawHeader("Authorization", requestWithInfo.header)))
    } map { response =>
      System.out.println("Response: " + response.entity.asString)
      parseRequestTokenResponse(response.entity.asString).right.get
    }
    val token = Await.result(requestTokenResponseF, 4 seconds)
    tokens.put(userId, token)
    redirect(AuthorizeTokenUrl + "?" + TokenName + "=" + token.token)
  }

  get("/accessToken") {
    val userId = params(UserId).toInt
    val requestToken = tokens.get(userId)
    val accessTokenResponseF = consumer.createAccessTokenRequest(
      KoauthRequest("POST", AccessTokenUrl, None), ConsumerKey, ConsumerSecret, requestToken.token, requestToken.secret,
        params(VerifierName)) flatMap { requestWithInfo =>
      pipeline(pipelining.Post(AccessTokenUrl).withHeaders(RawHeader("Authorization", requestWithInfo.header)))
    } map { response =>
      System.out.println("Response: " + response.entity.asString)
      parseAccessTokenResponse(response.entity.asString).right.get
    }
    val accessToken = Await.result(accessTokenResponseF, 4 seconds)
    tokens.put(userId, accessToken)
    redirect(s"/lastTweet?$UserId=$userId")
  }

  get("/lastTweet") {
    val lastTweetUrl = "https://api.twitter.com/1.1/statuses/user_timeline.json?count=1&include_rts=1&trim_user=true"
    val responseF = sign(pipelining.Get(lastTweetUrl), params(UserId).toInt).flatMap(pipeline(_))
    Await.result(responseF, 4 seconds).entity.asString
  }

  private def sign(request: HttpRequest, userId: Int) = {
    val body = request.headers
      .find(h => h.name == "Content-Type" && h.value == "application/x-www-form-urlencoded")
      .map(_ => request.entity.asString)
    val token = tokens.get(userId)
    consumer.createOauthenticatedRequest(KoauthRequest(request.method.value, request.uri.toString(), None, body),
        ConsumerKey, ConsumerSecret, token.token, token.secret) map { requestWithInfo =>
      request.withHeaders(RawHeader("Authorization", requestWithInfo.header))
    }
  }
}
