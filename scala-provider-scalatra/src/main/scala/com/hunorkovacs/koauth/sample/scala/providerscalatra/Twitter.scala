package com.hunorkovacs.koauth.sample.scala.providerscalatra

import javax.servlet.http.HttpServletRequest
import com.hunorkovacs.koauth.domain._
import com.hunorkovacs.koauth.domain.mapper.{ResponseMapper, RequestMapper}
import com.hunorkovacs.koauth.service.DefaultTokenGenerator.generateVerifier
import com.hunorkovacs.koauth.service.provider.ProviderServiceFactory
import com.hunorkovacs.koauth.service.provider.persistence.{RequestToken, ExampleMemoryPersistence}
import org.scalatra._

import scala.concurrent.duration._

import scala.concurrent.{Await, Future, ExecutionContext}

class Twitter extends ScalatraServlet {

  implicit private val ec = ExecutionContext.Implicits.global
  private val persistence = new MyExampleMemoryPersistence(ec)
  private val provider = ProviderServiceFactory.createProviderService(persistence, ec)
  private val requestMapper = new JavaxRequestMapper(ec)
  private val responseMapper = new JavaxResponseMapper(ec)

  private var token = TokenResponse("", "")

  post("/oauth/requestToken") {
    Await.result(mapCallMap(provider.requestToken), 4 seconds)
  }

  post("/oauth/authorize-with-password") {
    if (params("username") == "admin" && params("password") == "admin")
      Await.ready(persistence.authorizeRequestToken(params("requestToken"), params("username"), generateVerifier), 4 seconds)
    else
      Unauthorized("Credentials or token not valid.")
  }

  post("/oauth/accessToken") {
    Await.result(mapCallMap(provider.accessToken), 4 seconds)
  }

  get("/me") {
    val response =
      requestMapper.map(request) flatMap { koauthRequest =>
        provider.oauthenticate(koauthRequest)
      } map {
        case Left(result) => result match {
          case ResponseUnauthorized(body) => Unauthorized(body)
          case ResponseBadRequest(body) => BadRequest(body)
        }
        case Right(username) => Ok("You are " + username)
      }
    Await.result(response, 4 seconds)
  }

  private def mapCallMap(f: KoauthRequest => Future[KoauthResponse]) = {
    requestMapper.map(request)
      .flatMap(f)
      .flatMap(responseMapper.map)
  }
}

class JavaxRequestMapper(private val ec: ExecutionContext) extends RequestMapper[HttpServletRequest] {

  implicit private val implicitEc = ec

  override def map(source: HttpServletRequest) = {
    Future {
      val length = source.getContentLength
      val body =
        if (length > 0) {
          val body = new Array[Char](length)
          source.getReader.read(body)
          Some(new String(body))
        } else None
      KoauthRequest(source.getMethod, source.getRequestURL.toString, body)
    }
  }
}

class JavaxResponseMapper(private val ec: ExecutionContext) extends ResponseMapper[String] {

  implicit private val implicitEc = ec

  override def map(source: KoauthResponse): Future[ActionResult] = {
    Future {
      source match {
        case ResponseOk(body) => Ok(body)
        case ResponseUnauthorized(body) => Unauthorized(body)
        case ResponseBadRequest(body) => BadRequest(body)
        case _ => InternalServerError()
      }
    }
  }
}

class MyExampleMemoryPersistence(override val ec: ExecutionContext) extends ExampleMemoryPersistence(ec) {

  private val ConsumerKey = "OmFjJKNqU4v791CWj6QKaBaiEep0WBxJ"

  def authorizeRequestToken(requestToken: String, verifierUsername: String, verifier: String): Future[Unit] = {
    Future {
      requestTokens.find(t => t.requestToken == requestToken) match {
        case None => Unit
        case Some(token) =>
          requestTokens.indexOf(token)
          requestTokens += RequestToken(ConsumerKey, token.requestToken, token.requestTokenSecret, "",
            Some(verifierUsername), Some(verifier))
          Unit
      }
    }
  }
}