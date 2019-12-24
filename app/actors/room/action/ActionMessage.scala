package actors.room.action

import akka.actor.ActorRef

import scala.collection.mutable

case class ClientLose(uid:String,cid:String,roomId:String)
case class Boadcast(id: String,msg:Message,roomId:String="")
case class RoomClose(roomId:String="",text:String="")
case class RoomUserClientIn(uid: String,roomId:String,msg:Message)
case class RoomUserExit(id: String,roomId:String="")
case class RoomIn(roomId:String,room:ActorRef)
case class RoomUserKick(id: String,actUid:String,text:String,roomId:String="")
class Message(val actionType:String, val body:mutable.HashMap[String,Any], val time:Long) extends Serializable

object ClientMessageActionType {
  val USER_EXIT_ROOM = "user_exit_room"
  //
  val ROOM_CLOSE = "room_close" //text
  //
  val USER_SEND_MESSAGE = "user_send_message" //text,color,images,msgType
  val USER_SEND_GIFT = "user_send_gift" // giftId,count
}

object ServerMessageActionType {
  val USER_INTO_ROOM = "user_into_room" // userId,userName,level,color,icons,userCount
  val USER_EXIT_ROOM = "user_exit_room"
  // userCount,userId
  val USER_KICK_ROOM = "user_kick_room"
  //userId,userCount,actUid,text
  val ROOM_CLOSE = "room_close"
  //
  val USER_SEND_MESSAGE = "user_send_message" //userId,text,color,images
  val USER_SEND_GIFT = "user_send_gift" // userId,giftId,count,showTime
  val UNKNOWN_CONTENT_ERROE = "unknown_content_error" //data
}