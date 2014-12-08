package utils

import play.api.mvc.Results._
import play.api.mvc._
import play.api.Logger
import scala.concurrent._
import play.api._

/** Logs before each request is processed */
object LoggingAction extends ActionBuilder[Request] {
  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    Logger.info(s"processing ${request.path}")
    block(request)
  }
}

/** Authorizes requests using hmac in Authorization header */
case class HmacAuthAction(allowed: String, data: List[Any] = List()) extends ActionFilter[Request] {
  val boothSecret = Play.current.configuration.getString("booth.auth.secret").get
  val expiry = Play.current.configuration.getString("booth.auth.expiry").get.toInt
  val regexp = """\$\{([a-zA-Z0-9]+)\}""".r

  /** deny requests that dont pass hmac validations */
  def filter[A](input: Request[A]) = Future.successful {

    input.headers.get("Authorization").map(validate(input)) match {
      case Some(true) => None
      case _ => Some(Forbidden)
    }
  }

  /** validate an hmac authorization code */
  def validate[A](request: Request[A])(value: String): Boolean = {

    try {
      // expand index variables ($0 $1 etc)
      var expanded = allowed
      data.zipWithIndex.foreach { case(d, index) =>
        expanded = expanded.replace("$" + index, d.toString)
      }

      val split = value.split(':')
      val permission = split(0)
      val time = split(1).toLong
      val hash = split(2)
      val now = new java.util.Date().getTime
      val diff = now - time

      if( (diff < expiry) && (Crypto.hmac(boothSecret, s"$permission:$time") == hash) && (expanded == permission) ) {
        return true
      }

      return false
    }
    catch {
      case e:Exception => Logger.warn(s"Exception verifying hmac ($value)", e); false
    }
  }
}

/** pipeline: LoggingAction and then HmacAction */
case class LHAction(allowed: String, data: List[Any] = List()) extends ActionBuilder[Request] {
  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    (LoggingAction andThen HmacAuthAction(allowed, data)).invokeBlock(request, block)
  }
}