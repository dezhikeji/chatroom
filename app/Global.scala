
import java.io.File
import java.util.{Date, TimeZone}

import actors._
import akka.actor.Props
import common.Tool._
import db.{DBEntity, ExecuteSql}
import play.api.Play.current
import play.api._
import play.api.libs.concurrent.Akka
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.Future
import scala.io.Source

/**
 * Created by 林 on 14-4-8.
 */
object Global  extends  GlobalSettings {
  override  def onStart(app:Application)={
    Logger.info(TimeZone.getDefault.toString)
    val timezone = TimeZone.getTimeZone("GMT+8")
    TimeZone.setDefault(timezone)
    Logger.info(new Date().sdatetime)
    Logger.info("utf-8 test:这是中文")
    //启动消息发送Actor
    val taskServer=Akka.system.actorOf(Props[TaskServer], name = "task_server_actor")
    //启动sql创建系统
    executeSql()
    //启动系统
    super.onStart(app)
    taskServer ! new StartMessage
  }
  override def onError(request: RequestHeader, ex: Throwable) = {
    sendStackTrace(ex,request.toJson)
    Logger.info("error:"+request.toJson,ex)
    Future.successful(InternalServerError(ex.getMessage
    ))
  }
  override def onHandlerNotFound(request: RequestHeader) = {
    Future.successful(NotFound(
      request.path
    ))
  }
  //发送邮件给开发组
  def sendStackTrace(e:Throwable,msg:String)={
    Logger.info("find send out error mail",e)
  }

  /**
    * 执行sql
    */
  def executeSql(): Unit ={
    val sqls=new File("conf/sql").listFiles().map(f=> f.getName -> f)
    val allExecutes=getAllExecutes.toSet
    sqls.filter(f=> !allExecutes.contains(f._1)).foreach{f=>
      val sql=Source.fromFile(f._2,"utf-8").getLines().mkString
      sql.split(";").filter(v=> !isEmpty(v) && !isEmpty(v.trim) ).foreach{sqllite=>
        Logger.info("execute sql:"+f._1+",content:"+sqllite)
        DBEntity.sql(sqllite)
      }
      new ExecuteSql(f._1,sql,new Date()).insertWithId()
    }
  }
  def getAllExecutes={
    try{
      new ExecuteSql().queryAll().map(_.id)
    }catch {
      case _:Throwable=>
        Logger.info("no db info,create table:ExecuteSql")
        new ExecuteSql().createTable()
        Nil
    }
  }

}
