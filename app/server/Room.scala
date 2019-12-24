package server

import java.util
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

import actors.room._
import actors.room.action.{RoomClose, RoomHeadBag}
import akka.actor.{ActorIdentity, ActorRef, ActorSystem, Identify, Props}
import akka.pattern.AskableActorSelection
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import common.Tool
import controllers.BaseController
import db.{LiveRoom, User}
import play.api.Logger

import scala.concurrent.duration._
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.Await
/**
  * 房间集合
  */
object Room {
  lazy val conf = ConfigFactory.load()
  implicit val timeout = Timeout(5 seconds)
  var closeCount=0
  val startTime = System.currentTimeMillis() / 1000
  var socketCount = new AtomicInteger(0)
  var allSocketCount=new AtomicInteger(0)
  val systemUids =conf.getStringList("systemUid")
  val rooms = Collections.synchronizedMap[String, ActorRef](new util.HashMap[String, ActorRef]())
  val openRooms = Collections.synchronizedSet[String](new util.HashSet[String]())
  private val spamCache = new mutable.ListBuffer[String]()

  def props(out: ActorRef, user: User, ip: String,roomid:String ,room: ActorRef)(implicit system:ActorSystem) = {
    socketCount.addAndGet(1)
    allSocketCount.addAndGet(1)
    val cid=ip+","+Tool.uuid
    Props(new ClientActor(out, user, cid,roomid, getRemoteActor))
  }

  //给聊天室发送消息
  def sendMessage(roomId:String,message:Any)(implicit system:ActorSystem)={
     Logger.info(roomId+":"+message)
    getRemoteActor ! message
  }

  def cleanUserSocket(uid: String): Unit = {
    rooms.map(_._2).foreach(_ ! RoomHeadBag)
  }

  def cleanRoomSocket() { // 定时重启机制
    val dbRooms=new LiveRoom().query("status=1").map(_.uid)
    rooms.map(_._1).filter(v=> !dbRooms.contains(v)).foreach(r=> if(rooms.containsKey(r))rooms(r) ! RoomClose)
    val timeDiff = System.currentTimeMillis() / 1000 - startTime
    if (dbRooms.size == 0 && rooms.size() == 0 && allSocketCount.get() > 50000 && timeDiff > 3600) {
      System.exit(0)
    }
    if(dbRooms.size==0 && rooms.size() !=0){
      if(closeCount>2){
        System.exit(0)
      }else{
        closeCount=closeCount+1
      }
    }
  }


  /**
    * 获取或者创建聊天室
    *
    * @param id
    * @return
    */
  def getOrCreateRoom(id: String)(implicit system:ActorSystem) = {
    if (Room.rooms.containsKey(id)) {
      Room.rooms.get(id)
    } else {
      val ra = system.actorOf(Props(new RoomActor(id,BaseController.conf)), name = "room_" + id)
      Room.rooms.put(id, ra)
      ra
    }
  }
  def getRemoteActor(implicit system:ActorSystem)={
    getActor(conf.getString("remoteActor"),system)
  }
  private def getActor(path: String,system:ActorSystem) = {
    val actorFuture = system.actorSelection(path)
    val asker = new AskableActorSelection(actorFuture)
    val fut = asker.ask(new Identify(1))
    val ident = Await.result(fut, timeout.duration).asInstanceOf[ActorIdentity]
    ident.getRef
  }

  def getSpam(): List[String] = {
    spamCache.toList
  }

  def setSpam(data: List[String]) = {
    spamCache.clear()
    data.foreach(v => spamCache.append(v))
  }
  def checkSpam(str:String)={
    getSpam().foldLeft(false)((b,k)=>if(b) b else checkSpamKey(str,k))
  }
  def checkSpamKey(str:String,key:String):Boolean={
    if(key.contains("&")){
      key.split("&").filterNot(k=> str.contains(k)).isEmpty
    }else if(key.contains("|")){
      key.split("|").filter(k=> str.contains(k)).size >0
    }else str.contains(key)
  }
}
