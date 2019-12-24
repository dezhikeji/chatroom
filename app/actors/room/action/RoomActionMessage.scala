package actors.room.action

import akka.actor.ActorRef

/**
  * Created by admin on 4/25/2017.
  */
case class RoomHeadBag()
case class RoomClientIn(uid: String,cid:String,actor:ActorRef)