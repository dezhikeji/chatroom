package controllers

import javax.inject.Inject

import actors.room._
import actors.room.action._
import db._
import io.swagger.annotations._
import play.api.mvc._
import play.api.libs.streams._
import akka.actor._
import akka.stream._
import common.Tool._
import common.{OtsCache, VenusException}
import play.api.Logger
import server.Room
import tools._

import scala.collection.mutable.HashMap
import scala.concurrent.Future

/**
  * Created by 林 on 2015-10-9.
  */
@Api(value = "/api/chatroom")
class RoomController @Inject()(implicit system: ActorSystem, materializer: Materializer) extends BaseController {

  @ApiOperation(value = "聊天室控制", notes = "聊天室控制")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "token", value = "检验token", required = true, dataType = "string", paramType = "form"),
    new ApiImplicitParam(name = "action", value = "操作 (close,gift)", required = true, dataType = "string", paramType = "form"),
    new ApiImplicitParam(name = "room", value = "房间", required = true, dataType = "string", paramType = "form"),
    new ApiImplicitParam(name = "uid", value = "操作用户", required = true, dataType = "string", paramType = "form"),
    new ApiImplicitParam(name = "otherUid", value = "被操作用户", required = true, dataType = "string", paramType = "form"),
    new ApiImplicitParam(name = "giftId", value = "礼物id，仅限gift操作", required = false, dataType = "string", paramType = "form"),
    new ApiImplicitParam(name = "score", value = "魅力值，仅限gift操作", required = false, dataType = "string", paramType = "form"),
    new ApiImplicitParam(name = "type", value = "关闭类型，仅限close操作", required = false, dataType = "string", paramType = "form"),
    new ApiImplicitParam(name = "count", value = "礼物数量，仅限gift操作", required = false, dataType = "string", paramType = "form")))
  def control = JsAction { implicit request =>
    val List(token,room,action,uid,giftId,count,otherUid,closeType,score)=getParam("token","room","action","uid","giftId","count","otherUid","type","score")
    val realRoom=room.trim
    if (token== "chatroom".encrypt()) {
      Logger.info(s"new control into room ${realRoom}")
      if(checkRoom(realRoom) || action=="open") {
        Logger.info(s"action ${action} uid ${uid}")
        action match {
          case "close" =>
            Room.openRooms.remove(realRoom)
            val text=closeType match{
              case "1" => "主播接听私密视频去了"
              case "2"=> "主播信号不好，直播中断"
              case "3" => "鉴黄系统检测到非法内容，强制封禁直播室"
              case "4"=>  "直播室被管理员强制封锁"
              case _ => "主播关闭了直播室"
            }
            Room.sendMessage(realRoom,RoomClose(realRoom,text))
          case "open" =>
            Room.openRooms.add(realRoom)
            Room.getOrCreateRoom(realRoom)
          case "exit" =>
            Room.sendMessage(realRoom,RoomUserExit(uid,realRoom))
          case "kick" =>
            val user=new User().queryById(otherUid).dbCheck
            val actUser=new User().queryById(uid).dbCheck
            OtsCache.setExpCache("kick_user"+realRoom+"_"+otherUid,"1",300)
            val text=s"${actUser.name}把${user.name}踢出了直播间"
            Room.sendMessage(realRoom,RoomUserKick(otherUid,uid,text,realRoom))
          case "gift" =>
            val user = new User().queryById(uid).dbCheck
            val color=if(Room.systemUids.contains(user.id)) "#7CFC00"  else if(user.levelType >0) "#eb3723" else "#c7c7c7"
            val gift = new Gift().queryById(giftId).dbCheck
            Room.sendMessage(realRoom,Boadcast(user.id, new Message(ServerMessageActionType.USER_SEND_GIFT,
              HashMap("giftId" -> giftId, "giftAni" -> gift.ani, "giftIcon" -> gift.icon,  "giftName" -> gift.name,"contentColor"->"#FFFFFF",
                "userId" -> user.id, "msgType" -> "1", "userName" -> user.name,"level" -> user.levelType, "userIcon" -> user.icon, "price" -> gift.price,
                "color"->color, "count" -> count.toInt,"score" -> score.toLong, "showTime" -> count.toInt * 1000 *5), System.currentTimeMillis()),realRoom))
        }
        Ok(ajaxOk)
      }else{
        throw new VenusException("聊天室不存在"+realRoom)
      }
    } else {
      throw new VenusException("验证信息不正确"+token)
    }
  }

  /**
    * 客户端链接聊天室
    */
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "token", value = "用户token", required = true, dataType = "string", paramType = "query")))
  def socket(id: String) = WebSocket.acceptOrResult[String, String] { request =>
    val nid=id.trim
    val ip=request.remoteAddress
    val List(token,time)=List("token","time").map(v=>request.getQueryString(v).getOrElse(""))
    Future.successful {
      if(isEmpty(time)){
        Left(Forbidden)
      }else
      if ((System.currentTimeMillis() / 1000 ) - time.decrypt().toLong > 3600  && nid != token.decrypt().trim) {
        Left(Forbidden)
      } else {
        val user = new User().queryById(token.decrypt()).filter(_.status != 3)
        user match {
          case None =>
            Logger.info("socket block no user:" + token.decrypt())
            Left(Forbidden)
          case Some(u) =>
            if (OtsCache.getExpCache("kick_user" + nid + "_" + u.id).isDefined) {
              Left(Forbidden)
            } else if (Room.openRooms.contains(nid)) {
              Logger.info(s"new user ${u.id} into room ${nid}")
              Right(ActorFlow.actorRef { out => Room.props(out, u, ip, nid, Room.getOrCreateRoom(nid)) })
            } else {
              if (checkRoom(nid)) {
                Room.openRooms.add(nid)
                Right(ActorFlow.actorRef { out => Room.props(out, u, ip, nid, Room.getOrCreateRoom(nid)) })
              } else {
                Logger.info("socket block room close:" + token.decrypt())
                Left(Forbidden)
              }
            }
        }
      }
    }
  }

  def checkRoom(id:String)={
    if(!Room.openRooms.contains(id)){
      cacheStaticMethod("check_room",1){new LiveRoom().queryOne("uid=?",id).map(_.status==1).getOrElse(false).toString}.toBoolean
    } else true
  }
}

