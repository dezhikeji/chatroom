package actors.room.action

import common.Tool

import scala.collection.mutable

/**
  * Created by admin on 12/9/2016.
  */
object MessageDeal {
  def dealUserInput(msg:String): String ={
      val at=Tool.toBean(msg,classOf[Message])
     at.actionType match{
       case ClientMessageActionType.USER_EXIT_ROOM =>

     }
    ""
  }

}
