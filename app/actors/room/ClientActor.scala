package actors.room

import java.util.Date

import actors.room.action._
import akka.actor.{Actor, ActorRef, PoisonPill, Terminated}
import common.Tool._
import common.{OtsCache, Tool}
import db._
import play.api.Logger
import server.Room
import server.Room.{socketCount, systemUids}

import scala.collection.mutable.{HashMap, ListBuffer}

/**
  * 客户端部分
  *
  * @param out  输出流
  * @param user   用户
  * @param remoteRoomActor 房间id
  */
class ClientActor(out: ActorRef, user: User, cid:String, roomId:String, remoteRoomActor: ActorRef) extends Actor {
  var roomClose=false
  var lastMsg=""
  var spamCount=0
  var startSpam=0l
  var lastTime=System.currentTimeMillis()/1000
  var lastMsgTime=System.currentTimeMillis()
  val msgList=new ListBuffer[Tuple2[Long,Int]]()
  val color=if(Room.systemUids.contains(user.id)) "#7CFC00"  else if(user.levelType ==1 || user.levelType ==2)"#FF0000"
  else if(user.levelType ==3 || user.levelType ==4)"#F709C7" else if(user.levelType ==5 || user.levelType ==6|| user.levelType > 6)"#FFCC00" else "#FFFFFF"
  val contentColor=if(Room.systemUids.contains(user.id)) "#7CFC00"  else "#FFFFFF"
  implicit val system =context.system
  def userNumber(uid:String)=cacheStaticMethod("getuid_"+uid,3600*24*30){DBEntity.queryMap(s"select lid from ${new User().tableName} where id=?",uid).head("lid").toString}.toLong
  override def preStart() {
    Logger.info("clinet into:" + user.id + ":" + cid)
    super.preStart()
    context.watch(remoteRoomActor)
    Room.getOrCreateRoom(roomId) ! RoomClientIn(user.id,cid,self)
    val mount=OtsCache.getExpCache("user_mount_"+user.id).map(v=> toBean(v.toString,classOf[Map[String,Any]])).getOrElse(Map.empty[String,Any])
    val ani=mount.getOrElse("ani","")
    val name=mount.getOrElse("name","")
    val text=if(isEmpty(ani))"进入直播间" else "骑着【"+name+"】进入直播间"
    remoteRoomActor ! RoomUserClientIn(user.id, roomId,new Message(ServerMessageActionType.USER_INTO_ROOM, HashMap("userId" -> user.id, "userName" -> user.name, "level" -> user.levelType, "color" -> color,"ani"->ani,"text"->text,"contentColor"->contentColor), System.currentTimeMillis()))
    if (user.id == roomId) {
      out ! s"""{"actionType":"user_send_message","body":{"userId":"${user.id}","userName":"系统提示","msgType":"1","text":"${getSettingMap()("roomIntoMessage")}","color":"#7CFC00","contentColor":"${contentColor}","images":[]}}"""
    } else {
      out ! s"""{"actionType":"user_send_message","body":{"userId":"${user.id}","userName":"系统提示","msgType":"1","text":"${getSettingMap()("userIntoMessage")}","color":"#7CFC00","contentColor":"${contentColor}","images":[]}}"""
    }
  }

  def receive = {
    case msg: String =>
      lastTime=System.currentTimeMillis()/1000
      if(msg !="heartbeat" && msg !="heartbag"){
        try {
          val at = Tool.toBean(msg, classOf[Message])
          at.actionType match {
            case ClientMessageActionType.USER_SEND_MESSAGE =>
              val text=at.body("text").toString
              msgList.append((System.currentTimeMillis() -lastMsgTime )->text.length)
              lastMsgTime=System.currentTimeMillis()
              if(checkSpam()){
                spamCount=spamCount+1
                if(spamCount>50){
                  new User(user.id, status = 3).update("id", "status")
                  out ! PoisonPill
                }
              }
              val sendMsg=Boadcast(user.id, new Message(ServerMessageActionType.USER_SEND_MESSAGE, at.body += ("userId" -> user.id) += ("userName" -> user.name) += ("level" -> user.levelType) += ("color"->color) += ("msgType"->"0")+= ("contentColor"->contentColor), System.currentTimeMillis()),roomId)
              if(text.length>6 && Room.checkSpam(text)){
                spamCount=spamCount+1
                out ! s"""{"actionType":"user_send_message","body":{"userId":"${user.id}","msgType":"1","userName":"系统提示","text":"${"此消息已被反垃圾系统过滤，无法发送"}","color":"#7CFC00","contentColor"->"#FFFFFF","images":[]}}"""
                if(startSpam==0l) startSpam=System.currentTimeMillis()
//                if(spamCount>=10 && System.currentTimeMillis() -startSpam < (1000 * 60 *5)){
//                  new User(user.id, status = 3).update("id", "status")
//                  out ! PoisonPill
//                }
              } else if( systemUids.contains(user.id) || lastMsg != text) {
                remoteRoomActor ! sendMsg
                lastMsg =text
                safe {
                  new RoomMessageNew(0L,userNumber(roomId),userNumber(user.id),text,new Date()).insert()
                }
              }else {
                out ! sendMsg.msg.toJson()
              }
            case _ => out ! new Message(ServerMessageActionType.UNKNOWN_CONTENT_ERROE, HashMap("data" -> msg), System.currentTimeMillis()).toJson()
          }
        } catch {
          case _: Throwable => out ! new Message(ServerMessageActionType.UNKNOWN_CONTENT_ERROE, HashMap("data" -> msg), System.currentTimeMillis()).toJson()
        }
      }
    case Boadcast(_,msg,_)=>
      out ! msg.toJson()
    case RoomUserExit(_,_) =>
      out ! PoisonPill
      context.stop(self)
    case RoomUserKick(_,_,_,_) =>
      out ! PoisonPill
      context.stop(self)
    case RoomHeadBag=>
      if(System.currentTimeMillis() /1000 -lastTime >60){
        out ! PoisonPill
        context.stop(self)
      }
    case RoomClose(_,text)=>
      roomClose=true
      out ! new Message(ServerMessageActionType.ROOM_CLOSE,HashMap("text" -> text),System.currentTimeMillis()).toJson()
      self ! RoomUserExit("")
    case Terminated(_) =>
      //远程room无法连上了,自杀
      Logger.info("remote close")
      out ! PoisonPill
      roomClose=true
      context.stop(self)
    case e:Any =>  Logger.info("unknown msg:" + e)
  }

  override def postStop() = {
    Logger.info("socket lose or close :" + user.id+":"+cid)
    if(!roomClose) Room.getOrCreateRoom(roomId) ! ClientLose(user.id,cid,roomId)
    remoteRoomActor ! ClientLose(user.id,cid,roomId)
    socketCount.addAndGet(-1)
  }
  def checkSpam()={
    if(msgList.size<5) false else msgList.map(kv=> kv._2*1000 / kv._1).filter(_ > 1).size > (msgList.size * 0.8)
  }
}
