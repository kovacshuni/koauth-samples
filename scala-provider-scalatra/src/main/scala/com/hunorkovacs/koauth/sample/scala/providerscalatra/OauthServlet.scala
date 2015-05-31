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

class OauthServlet extends ScalatraServlet {

  implicit private val ec = ExecutionContext.Implicits.global
  private val persistence = new MyExampleMemoryPersistence(ec)
  private val provider = ProviderServiceFactory.createProviderService(persistence, ec)
  private val requestMapper = new JavaxRequestMapper(ec)
  private val responseMapper = new JavaxResponseMapper(ec)

  post("/oauth/request-token") {
    Await.result(mapCallMap(provider.requestToken), 4 seconds)
  }

  private def mapCallMap(f: KoauthRequest => Future[KoauthResponse]) = {
    requestMapper.map(request)
      .flatMap(f)
      .flatMap(responseMapper.map)
  }

  get("/oauth/authorize-with-password") {
    val username = params("username")
    if (username == "admin" && params("password") == "admin") {
      val requestToken = params("requestToken")
      val verifier = generateVerifier
      val callbackUrlF =
        persistence.authorizeRequestToken(requestToken, username, verifier) flatMap { _ =>
          persistence.getCallback(MyExampleMemoryPersistence.ConsumerKey, requestToken)
        }
      val callbackUrl = Await.result(callbackUrlF, 4 seconds)
      redirect(callbackUrl.get + "?" + OauthParams.VerifierName + "=" + verifier)
    } else
      Unauthorized("Credentials or token not valid.")
  }

  post("/oauth/access-token") {
    Await.result(mapCallMap(provider.accessToken), 4 seconds)
  }

  /**
   * But usually these endpoints that serve the main application logic shouldn't be designed exactly like this.
   * In order to save time and code duplication, and focus responsibilities, there should be a separate application
   * whose job is only the authentication verification that proxies all the normal endpoints. It does the first part
   * of the function below: provider.oauthenticate(). Forward incoming requests, complementing them with either the
   * username of the authenticated requester or mark that it couldn't be authenticated and should be handled as
   * a guest. These complemented requests, once gone through the authentication phase, hit the main application,
   * directly with the info of who is making them.
   */
  get("/me") {
    val response =
      requestMapper.map(request) flatMap { koauthRequest =>
        provider.oauthenticate(koauthRequest)
      } map {
        case Left(result) => result match {
          case ResponseUnauthorized(body) => Unauthorized("You are treated as a guest.\n" + body)
          case ResponseBadRequest(body) => BadRequest("You are treated as a guest.\n" + body)
        }
        case Right(username) => Ok("You are " + username + ".")
      }
    Await.result(response, 4 seconds)
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
      KoauthRequest(source.getMethod, source.getRequestURL.toString, Option(source.getHeader("Authorization")), body)
    }
  }
}

class JavaxResponseMapper(private val ec: ExecutionContext) extends ResponseMapper[ActionResult] {

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

class MyExampleMemoryPersistence(ec: ExecutionContext) extends ExampleMemoryPersistence(ec) {

  implicit private val implicitEc = ec

  def authorizeRequestToken(requestToken: String, verifierUsername: String, verifier: String) = {
    Future {
      requestTokens.find(t => t.requestToken == requestToken) match {
        case None => Unit
        case Some(token) =>
          requestTokens.remove(requestTokens.indexOf(token))
          requestTokens += RequestToken(MyExampleMemoryPersistence.ConsumerKey, token.requestToken,
            token.requestTokenSecret, "", Some(verifierUsername), Some(verifier))
          Unit
      }
    }
  }
}

object MyExampleMemoryPersistence {
  val ConsumerKey = "OmFjJKNqU4v791CWj6QKaBaiEep0WBxJ"
}