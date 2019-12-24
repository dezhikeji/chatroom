package controllers

import com.typesafe.config.ConfigFactory
import common.Tool._
import common.VenusException
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.mvc._
import tools._

import scala.concurrent._

/**
  * Created by 林 on 14-4-8.
  */
class BaseController extends Controller {

  //直接封装包含用户是否登录的消息以及当前时间4
  implicit def setting = {
    getSettingMap //++ getSessionMap + ("time" -> System.currentTimeMillis().toString)
  }

  //标识一个请求是否有登录的

  val ajaxOk = Map("code" -> 200, "msg" -> "ok").toJson

  //普通方法 ，父类里面拦截掉所有的子类方法干些屌爆的事情
  object JsAction extends ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
      try {
        block(request).map(_.withHeaders("Cache-Control" -> "no-cache").as("application/json; charset=utf-8"))
      } catch {
        case e: Exception =>
          if(!e.isInstanceOf[VenusException]){
            Logger.error(e.getMessage, e)
            e.printStackTrace()
          }else Logger.info(e.getMessage)
          Future.successful(Status(500)(Map("code" -> 500, "msg" -> e.getMessage).toJson).withHeaders("Cache-Control" -> "no-cache").as("application/json; charset=utf-8"))
      }
    }
  }

  object Action extends ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
      try {
        block(request)
      } catch {
        case e: Exception =>
          if(!e.isInstanceOf[VenusException]){
            Logger.error(e.getMessage, e)
            e.printStackTrace()
          }else Logger.info(e.getMessage)
          Future.successful(Status(500)("ERROR"))
      }
    }
  }

  object UserAction extends ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
      request.session.get("uid") map {
        name =>
          try {
            block(request).map(_.withHeaders("Cache-Control" -> "no-cache").as("application/json; charset=utf-8"))
          } catch {
            case e: Exception => e.printStackTrace()
              if(!e.isInstanceOf[VenusException]){
                Logger.error(e.getMessage, e)
                e.printStackTrace()
              }else Logger.info(e.getMessage)
              Future.successful(Status(500)(Map("code" -> 500, "msg" -> e.getMessage).toJson).withHeaders("Cache-Control" -> "no-cache").as("application/json; charset=utf-8"))
          }
      } getOrElse {
        Future.successful(Status(401)(Map("code" -> 401, "msg" -> "登录已过期，请重新登录", "t" -> System.currentTimeMillis() / 1000).toJson).as("application/json; charset=utf-8"))
      }
    }
  }

  //发送邮件给开发组
  def sendStackTrace(e: Throwable, msg: String) = {
    Logger.info("find send out error mail", e)
  }

  //先从query里面取，取不到从body里面取
  def getParam(data: String*)(implicit request: Request[AnyContent]) = {
    data map (s => request.getQueryString(s).getOrElse {
      if (request.body.asFormUrlEncoded.isEmpty) ""
      else request.body.asFormUrlEncoded.get.getOrElse(s, List(""))(0)
    }) toList
  }


  def ip(implicit request: Request[Any]) = {
    if (request.headers.get("x-forwarded-for").isEmpty) {
      (request.remoteAddress).trim
    } else {
      request.headers.get("x-forwarded-for").get
    }
  }

  def base64(data: Array[Byte]): String = {
    if (isEmpty(data)) {
      null
    } else {
      new String(Base64.encode(data))
    }
  }

  def unBase64(data: String) = {
    if (isEmpty(data)) {
      null
    } else {
      Base64.decode(data.getBytes)
    }
  }

  def getPageWithList[A](list: List[A], page: Int, size: Int) = {
    if (list.size > 0) list.drop((page - 1) * size).take(size) else list
  }

  def money(i: Long) = {
    BigDecimal(i) / 100 toMoney()
  }
  def conf=BaseController.conf
}

object BaseController{
  //全局变量定义
  lazy val conf = ConfigFactory.load()
}
