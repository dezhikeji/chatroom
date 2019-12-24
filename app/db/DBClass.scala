package db

import java.text.SimpleDateFormat
import java.util.{Date, UUID}

import common._
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field
import scala.collection.mutable.HashMap
import common.TimeTool._


/**
  * Created by 林 on 14-3-26.
  */

class BaseDBEntity[+Self <: BaseDBEntity[Self]](tableName: String) extends DBEntity(tableName) {

  def uuid = UUID.randomUUID().toString.replace("-", "")

  def toJson: String = {
    BaseDBEntity.map.writeValueAsString(this)
  }

  def toMap: Map[String, Any] = {
    BaseDBEntity.map.readValue(toJson, Map[String, Any]().getClass).asInstanceOf[Map[String, Any]]
  }

  def toHashMap: HashMap[String, Any] = {
    BaseDBEntity.map.readValue(toJson, HashMap[String, Any]().getClass).asInstanceOf[HashMap[String, Any]]
  }

  def fromJson(json: String): Self = {
    BaseDBEntity.map.readValue(json, this.getClass).asInstanceOf[Self]
  }

  //将对应的更新类转为实体类
  def changeUpdateBean(): Self = {
    fromJson(toJson)
  }

  override def queryById(id: String): Option[Self] = {
    super.queryById(id) map (_.asInstanceOf[Self])
  }

  override def quickByIds(ids: List[String]): List[Self] = {
    super.quickByIds(ids) map (_.asInstanceOf[Self])
  }

  override def quickById(id: String, onlyCache: Boolean = false): Option[Self] = {
    super.quickById(id, onlyCache) map (_.asInstanceOf[Self])
  }

  override def queryByIds(ids: List[String]): List[Self] = {
    super.queryByIds(ids) map (_.asInstanceOf[Self])
  }


  override def queryOne(sql: String, param: AnyRef*): Option[Self] = {
    super.queryOne(s"select * from $tableName where " + sql, param: _*) map (_.asInstanceOf[Self])
  }

  override def queryAll(): List[Self] = {
    super.queryAll map (_.asInstanceOf[Self])
  }

  override def query(where: String, param: AnyRef*): List[Self] = {
    super.query(where, param: _*) map (_.asInstanceOf[Self])
  }

  //这个接口需要传条件、排序
  override def queryPage(where: String, pageNum: Int, pageSize: Int, order: String, param: AnyRef*): (Int, List[Self]) = {
    val (count, list) = super.queryPage(where, pageNum, pageSize, order, param: _*)
    (count, list map (_.asInstanceOf[Self]))
  }

  /**
    * 从缓存中获取所有数据
    * 表有更新会自动删除
    *
    * @return
    */
  def queryAllWithCache() = Tool.cacheMethod("dbcache_" + tableName, 3600) {
    queryAll()
  }

}

object BaseDBEntity {
  protected val map = new ObjectMapper() with ScalaObjectMapper
  map.registerModule(DefaultScalaModule)
  map.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
  map.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm"))


  def toJson(data: AnyRef) = {
    map.writeValueAsString(data)
  }

  def toHashMap(dbe: DBEntity): HashMap[String, Any] = BaseDBEntity.map.readValue(map.writeValueAsString(dbe), HashMap[String, Any]().getClass).asInstanceOf[HashMap[String, Any]]

  //自动检查数据的查询结果是否存在
  implicit class DBOptionAdd[T <: DBEntity](o: Option[T]) {
    def dbCheck: T = if (o.isEmpty) throw new VenusException("数据不存在") else o.get
  }

  def uuid = UUID.randomUUID().toString.replace("-", "")

}

import BaseDBEntity.uuid


/**
  * 注释说明   表级别
  * {"method":"get,post,put,delete"(管理后台中的方法),"ref":"quick" or "cache"(获取关联表的方式),"map":["lat","lng"](地图属性的经纬度)}
  *
  */


/**
  * 系统设置表，系统中重要设置全部存储于此表中
  *
  */
//系统参数
@ApiModel(value = "Setting", description = "系统设置")
class Setting(val id: String = uuid,
              @(ApiModelProperty@field)(value = "键", required = true)
              val name: String = "",
              @(ApiModelProperty@field)(value = "值", required = true)
              val value: String = "",
              @(ApiModelProperty@field)(value = "备注")
              val remark: String = "") extends BaseDBEntity[Setting]("Setting")

@ApiModel(value = "ExecuteSql", description = "执行过的sql")
class ExecuteSql(
                  @(ApiModelProperty@field)(value = "路径")
                  val id: String = "",
                  @(ApiModelProperty@field)(value = "内容")
                  val content: String = "",
                  @(ApiModelProperty@field)(value = "执行时间", required = true, hidden = true)
                  val createTime: Date = new Date(System.currentTimeMillis())
                ) extends BaseDBEntity[ExecuteSql]("ExecuteSql")


@ApiModel(value = "User", description = "用户")
class User(val id: String = uuid,
           @(ApiModelProperty@field)(value = "名称", required = true)
           val name: String = "",
           @(ApiModelProperty@field)(value = "头像", required = true)
           val icon: String = "",
           @(ApiModelProperty@field)(value = "性别(0=女，1=男, -1=未知)", required = true)
           val sex: Int = -1,
           @(ApiModelProperty@field)(value = "密码", required = true, hidden = true)
           val pwd: String = "",
           @(ApiModelProperty@field)(value = "状态(0=未认证,1=认证中,2=已认证,3=已封禁)", required = true)
           val status: Int = 0,
           @(ApiModelProperty@field)(value = "会员类型(0=一般用户,1=主播,2=公会)", required = true)
           val userType: Int = 0,
           @(ApiModelProperty@field)(value = "vip类型(0=一般用户,1-5 vip等级)", required = true)
           val levelType: Int = 0,
           @(ApiModelProperty@field)(value = "云信登陆token", required = true)
           val imToken: String = "",
           @(ApiModelProperty@field)(value = "个人签名", required = true)
           val description: String = "",
           @(ApiModelProperty@field)(value = "会员过期时间", required = false)
           val levelDate: Date = null,
           @(ApiModelProperty@field)(value = "创建时间", required = false, hidden = true)
           val createTime: Date = new Date()) extends BaseDBEntity[User]("Users")

@ApiModel(value = "UserSetting", description = "用户设置")
class UserSetting(val id: String = uuid,
                  @(ApiModelProperty@field)(value = "邀请码", required = true)
                  val inviteCode: String = "",
                  @(ApiModelProperty@field)(value = "真实姓名", required = true)
                  val realName: String = "",
                  @(ApiModelProperty@field)(value = "身份证号码", required = true)
                  val idCard: String = "",
                  @(ApiModelProperty@field)(value = "认证照片", required = true)
                  val verifyPic: String = "",
                  @(ApiModelProperty@field)(value = "创建时间", required = false, hidden = true)
                  val createTime: Date = new Date()) extends BaseDBEntity[UserSetting]("UserSetting")


@ApiModel(value = "LiveRoomUser", description = """直播室用户""")
class LiveRoomUser(val id: String = uuid,
                   @(ApiModelProperty@field)(value = "用户", required = true, reference = "User")
                   val uid: String = "",
                   @(ApiModelProperty@field)(value = "直播室")
                   val roomId: String = "",
                   @(ApiModelProperty@field)(value = "名称", required = true)
                   val name: String = "",
                   @(ApiModelProperty@field)(value = "图片", required = true)
                   val icon: String = "",
                   @(ApiModelProperty@field)(value = "分数", required = true)
                   val score: Int = 0,
                   @(ApiModelProperty@field)(value = "个人签名", required = true)
                   val description: String = "",
                   @(ApiModelProperty@field)(value = "创建时间")
                   val createTime: Date = new Date) extends BaseDBEntity[LiveRoomUser]("LiveRoomUser")

@ApiModel(value = "LiveRoom", description = """直播室""")
class LiveRoom(val id: String = uuid,
               @(ApiModelProperty@field)(value = "用户", required = true, reference = "User")
               val uid: String = "",
               @(ApiModelProperty@field)(value = "主播编号", required = true)
               val number: String = "",
               @(ApiModelProperty@field)(value = "名称", required = true)
               val name: String = "",
               @(ApiModelProperty@field)(value = "图片", required = true)
               val icon: String = "",
               @(ApiModelProperty@field)(value = "主播等级", required = true)
               val level: Int = 0,
               @(ApiModelProperty@field)(value = "分数", required = true)
               val stat: Int = 0,
               @(ApiModelProperty@field)(value = "等级过滤", required = true)
               val levelFilter: Int = 0,
               @(ApiModelProperty@field)(value = "账户余额过滤", required = true)
               val amountFilter: Int = 0,
               @(ApiModelProperty@field)(value = "状态(0=未开播  1=开播中 2=开播异常状态 3=已封禁)", required = true)
               val status: Int = 0,
               @(ApiModelProperty@field)(value = "纬度", required = true)
               val lat: Double = 0d,
               @(ApiModelProperty@field)(value = "经度", required = true)
               val lng: Double = 0d,
               @(ApiModelProperty@field)(value = "在线人数", required = true)
               val userCount: Int = 0,
               @(ApiModelProperty@field)(value = "讨论组ID", required = true)
               val gid: String = "",
               @(ApiModelProperty@field)(value = "公会id", required = true)
               val guild: String = "",
               @(ApiModelProperty@field)(value = "扩展数据")
               val ext: String = "",
               @(ApiModelProperty@field)(value = "创建时间")
               val createTime: Date = new Date) extends BaseDBEntity[LiveRoom]("LiveRoom")


@ApiModel(value = "Gift", description = """礼物""")
class Gift(val id: String = uuid,
           @(ApiModelProperty@field)(value = "名称", required = true)
           val name: String = "",
           @(ApiModelProperty@field)(value = "图片", required = true)
           val icon: String = "",
           @(ApiModelProperty@field)(value = "动画", required = true)
           val ani: String = "",
           @(ApiModelProperty@field)(value = "价格", required = true)
           val price: Int = 0,
           @(ApiModelProperty@field)(value = "状态(0=未启用,1=已启用)", required = true)
           val status: Int = 0,
           @(ApiModelProperty@field)(value = "创建时间")
           val createTime: Date = new Date) extends BaseDBEntity[Gift]("Gift")

@ApiModel(value = "GiftUse", description = """礼物赠送""")
class GiftUse(val id: String = uuid,
              @(ApiModelProperty@field)(value = "礼物", required = true)
              val gid: String = "",
              @(ApiModelProperty@field)(value = "赠送用户", required = true)
              val uid: String = "",
              @(ApiModelProperty@field)(value = "赠送直播室", required = true)
              val lid: String = "",
              @(ApiModelProperty@field)(value = "数量", required = true)
              val amount: Int = 0,
              @(ApiModelProperty@field)(value = "创建时间")
              val createTime: Date = new Date) extends BaseDBEntity[GiftUse]("GiftUse")


@ApiModel(value = "RoomMessage", description = """聊天室消息""")
class RoomMessage(val id: String = uuid,
                  @(ApiModelProperty@field)(value = "房间id", required = true)
                  val roomId: String = "",
                  @(ApiModelProperty@field)(value = "用户id", required = true)
                  val uid: String = "",
                  @(ApiModelProperty@field)(value = "消息内容", required = true)
                  val message: String = "",
                  @(ApiModelProperty@field)(value = "扩展数据", required = true)
                  val ext: String = "",
                  @(ApiModelProperty@field)(value = "创建时间")
                  val createTime: Date = new Date) extends BaseDBEntity[RoomMessage]("RoomMessage")

@ApiModel(value = "RoomMessage", description = """聊天室消息""")
class RoomMessageNew(val id: Long = 0L,
                     @(ApiModelProperty@field)(value = "房间id", required = true)
                     val roomId: Long = 0L,
                     @(ApiModelProperty@field)(value = "用户id", required = true)
                     val uid: Long = 0L,
                     @(ApiModelProperty@field)(value = "消息内容", required = true)
                     val message: String = "",
                     @(ApiModelProperty@field)(value = "创建时间")
                     val createTime: Date = new Date) extends BaseDBEntity[RoomMessageNew]("RoomMessageNew")

@ApiModel(value = "SpamKey", description = "反垃圾敏感词")
class SpamKey(val id: String = uuid,
              @(ApiModelProperty@field)(value = "敏感词", required = true)
              val value: String = "",
              @(ApiModelProperty@field)(value = "创建时间", required = false, hidden = true)
              val createTime: Date = new Date()) extends BaseDBEntity[SpamKey]("SpamKey")