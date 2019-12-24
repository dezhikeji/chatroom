package actors

import java.util.{Calendar, Date}

import actors.room.action.RoomHeadBag
import akka.actor._
import akka.util.Timeout
import common.Tool._
import common._
import controllers._
import db._
import play.api.Logger
import server.Room
import server.Room.allSocketCount

import scala.collection.mutable
import scala.concurrent.duration._
import scala.collection.JavaConversions._
/**
  * Created by 林 on 14-4-10.
  */


class TaskServer extends Actor with ActorLogging {
  implicit val timeout = Timeout(60 seconds)

  def receive = {
    case st:StartMessage=>
      val settingMap = new Setting().queryAll.map(s => s.name -> s.value).toMap
      Tool.setSetting(settingMap)
      self ! new LoopTaskMessage
    case s: LoopTaskMessage => //定时任务
      safe {
        val dateTime = Calendar.getInstance()
        dateTime.setTime(new Date())
        val List(hour, minute, second) = List(dateTime.get(Calendar.HOUR_OF_DAY), dateTime.get(Calendar.MINUTE), dateTime.get(Calendar.SECOND))
        if (second % 29 == 0) {
          //更新缓存数据
          val settingMap = new Setting().queryAll.map(s => s.name -> s.value).toMap
          Tool.setSetting(settingMap)
          val spamList = new SpamKey().queryAll.map(_.value)
          Room.setSpam(spamList)
        }
        if (second % 55 == 0) {
          //打印日志
          println(new Date().sdatetime +"  room count:"+Room.rooms.size+",socket count:"+Room.socketCount.get()+",all socket count:"+allSocketCount.get())
        }
        if (minute % 5 ==0 && second % 42 == 0) {
          //清理无用连接
          run{
            Room.cleanRoomSocket
          }
        }
        if (second % 35 == 0) {
          //清理缓存数据
          Cache.cleanCache
        }
        if (second % 20 == 0) {
          //发送心跳包
          //Room.rooms.values().toArray.foreach(v=> v.asInstanceOf[ActorRef] ! RoomHeadBag)
        }
      }
      Thread.sleep(1000)
      self ! new LoopTaskMessage
    case uq: VenusException =>
      log.error(uq, "收到一个来自自身或者其他服务的不支持请求")
    case a: Any =>
      sender ! new VenusException(a.toString)
  }
}

case class LoopTaskMessage()

case class StartMessage()

