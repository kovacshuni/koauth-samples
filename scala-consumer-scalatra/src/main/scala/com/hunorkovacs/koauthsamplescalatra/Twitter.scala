package com.hunorkovacs.koauthsamplescalatra

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

  private val ConsumerKey = "OmFjJKNqU4v791CWj6QKaBaiEep0WBxJ"
  private val ConsumerSecret = "wr1KLYYH6o5yKFfiyN9ysKkPXcIAim2S"
  private val RequestTokenUrl = "http://localhost:9000/oauth/request-token"
  private val AuthorizeTokenUrl = "http://localhost:9000/oauth/authorize-with-password"
  private val AccessTokenUrl = "http://localhost:9000/oauth/access-token"

  implicit private val system = ActorSystem()
  import system.dispatcher
  private val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
  private val consumer = new DefaultConsumerService(system.dispatcher)
  private var token = TokenResponse("", "")

  get("/requestToken") {
    val requestTokenResponseF = consumer.createRequestTokenRequest(
        KoauthRequest("POST", RequestTokenUrl, None), ConsumerKey, ConsumerSecret,
          "http://127.0.0.1:8080/accessToken") flatMap { requestWithInfo =>
      pipeline(pipelining.Post(RequestTokenUrl).withHeaders(RawHeader("Authorization", requestWithInfo.header)))
    } map { response =>
      System.out.println("Response: " + response.entity.asString)
      parseRequestTokenResponse(response.entity.asString).right.get
    }
    token = Await.result(requestTokenResponseF, 4 seconds)
    redirect(AuthorizeTokenUrl + "?username=admin&password=admin&requestToken=" + token.token)
  }

  get("/accessToken") {
    val accessTokenResponseF = consumer.createAccessTokenRequest(
      KoauthRequest("POST", AccessTokenUrl, None), ConsumerKey, ConsumerSecret, token.token, token.secret,
        params(VerifierName)) flatMap { requestWithInfo =>
      pipeline(pipelining.Post(AccessTokenUrl).withHeaders(RawHeader("Authorization", requestWithInfo.header)))
    } map { response =>
      System.out.println("Response: " + response.entity.asString)
      parseAccessTokenResponse(response.entity.asString).right.get
    }
    token = Await.result(accessTokenResponseF, 4 seconds)
    redirect("/you")
  }

  get("/you") {
    val url = "http://localhost:9000/me"
    val responseF = consumer.createOauthenticatedRequest(KoauthRequest("GET", url, None),
        ConsumerKey, ConsumerSecret, token.token, token.secret) flatMap { requestWithInfo =>
      pipeline(pipelining.Get(url).withHeaders(RawHeader("Authorization", requestWithInfo.header)))
    }
    Await.result(responseF, 4 seconds).entity.asString
  }
}
