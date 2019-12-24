package actors.room

import actors.room.action.{RoomClose, _}
import akka.actor.{Actor, ActorRef, _}
import com.typesafe.config.Config
import play.api.Logger
import server.Room
import common.Tool._
import tools._

import scala.collection.mutable
import scala.collection.mutable.HashMap

/**
  * 房间部分
  */
class RoomActor(roomid: String, conf: Config) extends Actor {
  val userActor = new HashMap[String, mutable.HashMap[String, ActorRef]]()
  implicit val system =context.system
  override def preStart() {
    Logger.info("room  create:" + roomid)
    super.preStart()
    val remoteRoomActor=Room.getRemoteActor
    context.watch(remoteRoomActor)
    remoteRoomActor ! RoomIn(roomid,self)
  }
  def receive = {
    case RoomClientIn(uid, cid, act) =>
      Logger.info(s"client in:  ${roomid} ,${act.toString()}")
      if (!userActor.contains(uid)) {
        userActor(uid) = new mutable.HashMap[String,ActorRef]()
      }
      userActor(uid)(cid) = (act)
    case Boadcast(id, msg, _) =>
      Logger.info(s"room boadcast msg:  ${roomid} ,${msg.toJson()}")
      userActor.values.foreach(u=> u.values.map(c=>  c ! Boadcast(id, msg, roomid)))
    case RoomUserExit(id, _) =>
        Logger.info(s"room user exit:  ${roomid} ,${id}")
        if (userActor.contains(id)) {
          userActor(id).values.map(_ ! RoomUserExit(id,roomid))
        }
        userActor.remove(id)
    case RoomUserKick(id, _, _, _) =>
        Logger.info(s"room user kick:  ${roomid} ,${id}")
        if (userActor.contains(id)) {
          userActor(id).values.map(_ ! RoomUserKick(id,"","",""))
          userActor.remove(id)
        }
    case ClientLose(uid, cid,_) =>
      if (userActor.contains(uid)) {
        userActor(uid).remove(cid)
        if (userActor(uid).size == 0) {
          userActor.remove(uid)
        }
      }
    case RoomClose(_,text) =>
      Logger.info("room close:" + roomid)
      try {
        userActor.values.foreach(_.values.map(_ ! RoomClose(roomid,text)))
      } catch {
        case _: Exception =>
      }
      Room.rooms.remove(roomid)
      context.stop(self)
    case Terminated(_) =>
      //远程room无法连上了,自杀
      Logger.info("remote close")
      Room.rooms.remove(roomid)
      context.stop(self)
    case RoomHeadBag =>
      try {
        userActor.values.foreach(_.values.map(_ ! RoomHeadBag))
      } catch {
        case _: Exception =>
      }
  }
}
