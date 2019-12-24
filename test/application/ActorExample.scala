package application

import java.util.{Calendar, Date}

import actors.{LoopTaskMessage, StartMessage}
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.util.Timeout
import common.Tool._
import common.{Cache, Tool, VenusException}
import db._
import server.Room
import server.Room.allSocketCount

import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer
import scala.util.Random



class RoomMessageManager extends Actor with ActorLogging {
  implicit val timeout = Timeout(60 seconds)

  val childrens=(0 to 80).map(i=>context.actorOf(Props[RoomMessageServer], name = "child"+i))
  var exitInt=0
  def receive = {
    case rm:RoomMessage=>
        childrens(Random.nextInt(childrens.size)) ! rm
    case list: List[RoomMessage] =>
      childrens(Random.nextInt(childrens.size)) ! list
    case "extok"=>  exitInt +=1
      if(exitInt == childrens.size) System.exit(0)
    case "exit"=> childrens.foreach(_ ! "exit")
    case uq: VenusException =>
      log.error(uq, "收到一个来自自身或者其他服务的不支持请求")
    case a: Any =>
      sender ! new VenusException(a.toString)
  }
}




class RoomMessageServer extends Actor with ActorLogging {
  implicit val timeout = Timeout(60 seconds)

  def receive = {
    case rm:RoomMessage=>
      safe {
        new RoomMessageNew(0L, ActorExample.userNumber(rm.roomId), ActorExample.userNumber(rm.uid), rm.message, rm.createTime).insert()
        rm.delete("id")
      }
    case list: List[RoomMessage] =>
      safe {
        val sql = list.map { d =>
          s"""INSERT INTO `RoomMessageNew` (`roomId`, `uid`, `message`, `createTime`) VALUES ('${ActorExample.userNumber(d.roomId)}', '${ActorExample.userNumber(d.uid)}', "${d.message}", '${d.createTime.sdatetime}');\r\n""" +
            s"delete from RoomMessage where id='${d.id}';"
        }.mkString("\r\n")
        if(!isEmpty(sql.trim)){
          DBEntity.sql(sql)
        }
      }
    case "exit"=> sender ! "extok"
    case uq: VenusException =>
      log.error(uq, "收到一个来自自身或者其他服务的不支持请求")
    case a: Any =>
      sender ! new VenusException(a.toString)
  }
}




object ActorExample {

  def insert(rm:RoomMessage)={
    safe {
      new RoomMessageNew(0L, ActorExample.userNumber(rm.roomId), ActorExample.userNumber(rm.uid), rm.message, rm.createTime).insert()
      rm.delete("id")
    }
  }

  def inserList(list: List[RoomMessage])={
    safe {
      val sql = list.map { d =>
        s"""INSERT INTO `RoomMessageNew` (`roomId`, `uid`, `message`, `createTime`) VALUES ('${ActorExample.userNumber(d.roomId)}', '${ActorExample.userNumber(d.uid)}', "${d.message}", '${d.createTime.sdatetime}');\r\n""" +
          s"delete from RoomMessage where id='${d.id}';"
      }.mkString("\r\n")
      if(!isEmpty(sql.trim)){
        DBEntity.sql(sql)
      }
    }
  }

  def userNumber(uid:String)=cacheStaticMethod("number_"+uid,3600*24){ new UserSetting().queryById(uid).map(_.inviteCode).getOrElse("0")} toInt
  var exit=false
  def main(args: Array[String]): Unit = {
    val system = ActorSystem.create("PaiPai")
    val userActor = system.actorOf(Props[RoomMessageManager], name = "manager_actor")
    while(!exit){
      val time=System.currentTimeMillis()/1000
      val datas=DBEntity.query(classOf[RoomMessage],"select id,roomId,uid,message,createTime from RoomMessage limit 10000") // new RoomMessage().queryPage("",1,10000,"")._2
      if(datas.size==0){
        exit=true
        userActor ! "exit"
        println("wait exit")
      }
      else {
        println((System.currentTimeMillis() / 1000 - time) + ":" + datas.size)
        datas.filter(v => v.message.contains("\n") || v.message.contains("\"") || v.message.contains("\\")).mutile(40).foreach(v => insert(v))
        datas.filterNot(v => v.message.contains("\n") || v.message.contains("\"") || v.message.contains("\\")).grouped(100).toList.mutile(40).foreach(l => inserList(l))
        println(System.currentTimeMillis() / 1000 - time)
      }
    }
//    System.exit(0)

//    val spamList = new SpamKey().queryAll.map(_.value)
//    Room.setSpam(spamList)
//    println("主播&信".split("&").toJson())
//    println(Room.checkSpam("黄播网站e55m.com"))
//    lazy val conf = ConfigFactory.load()
//      Room.rooms.remove("123")
//      val in=new BufferedReader(new InputStreamReader(System.in))
//    while(true){
//      val text=in.readLine()
//      msgList.append((System.currentTimeMillis() -lastMsgTime )->text.length)
//      lastMsgTime=System.currentTimeMillis()
//      println(checkSpam)
//    }

//    val mqsend=new MQS("PID_zbtest",conf.getString("ali.accessId"),
//      conf.getString("ali.accessKey"),conf.getString("mq.add"),"zbroom-test")

//    val mq=new MQR("CID_zbtest",conf.getString("ali.accessId"),
//      conf.getString("ali.accessKey"),conf.getString("mq.add"),"zbroom-test")


//    mq.subscribe("1",{data=>
//      println("1:"+new String(data))
//    })

//    val mq2=new MQR("CID_zbtest",conf.getString("ali.accessId"),
//      conf.getString("ali.accessKey"),conf.getString("mq.add"),"zbroom-test")
//


//    mq.subscribe("3",{data=>
//      println("3:"+new String(data))
//    })


//    val mqs=new MQR("CID_zbtest",conf.getString("ali.accessId"),
//      conf.getString("ali.accessKey"),conf.getString("mq.add"),"zbroom-test")
//
//    mqs.subscribeMessage("*",{msg=>
//      println("*:"+msg.getTag+","+new String(msg.getBody))
//    })
//    Thread.sleep(1000)


//  1 to 5 foreach {i=>
//    mqsend.send("1",i.getBytes,i)
//  }
//    val s=Source.fromInputStream(System.in)
//    while(s.hasNext){
//      s.getLines().foreach{l=>
//        mqsend.send(l.split(",").head,l.getBytes,Tool.uuid)
//      }
//
//    }
//    mqs.send("2","6".getBytes,4)
    //    println(new RoomMessage().createTableSql())
//
//    Cache.setCache("a","1",5)
//    println(Cache.getCache("a").getOrElse("-1"))
//    Thread.sleep(3000)
//    println(Cache.getCache("a").getOrElse("-1"))
//    Thread.sleep(3000)
//    println(Cache.getCache("a").getOrElse("-1"))
//    Thread.sleep(3000)
//    println(Cache.getCache("a").getOrElse("-1"))




    //    println("chatroom".encrypt())
    //      new Role().queryAll().map{r=>
    //          if(r.method=="delete"&& !r.path.contains("{id}")){
    //            println(r.toJson)
    //            new Role(r.id,path = r.path+"/{id}").update("id","path")
    //          }
    //      }

    //      new User().quickById("f7fff25cf55b4cc9bf59ac2f0f3e95c6")
    //      val ne=new NetEase("eff7984c4d665721f809f97a083b9f1b","0c6a6ab6784e")
    //      ne.createGroup("f7fff25cf55b4cc9bf59ac2f0f3e95c6","喵群",Nil,"","介绍","邀请你加入群")
    ////      println(ne.getUser("f7fff25cf55b4cc9bf59ac2f0f3e95c6"))
    //      ne.systemMessage("f7fff25cf55b4cc9bf59ac2f0f3e95c6","这是狗屎2","200")
    //      println(SystemMessage.UserDelete)
    //      println(Map(status -> 0, environment -> Sandbox,
    //        receipt -> Map(version_external_identifier -> 0, app_item_id -> 0, request_date -> 2016-12-05 09:44:47 Etc/GMT, original_purchase_date -> 2013-08-01 07:00:00 Etc/GMT, original_purchase_date_ms -> 1375340400000, original_purchase_date_pst -> 2013-08-01 00:00:00 America/Los_Angeles, receipt_creation_date -> 2016-12-05 09:44:41 Etc/GMT, receipt_creation_date_ms -> 1480931081000, receipt_type -> ProductionSandbox, download_id -> 0, application_version -> 1, bundle_id -> com.dz.ppfinance, request_date_ms -> 1480931087511, request_date_pst -> 2016-12-05 01:44:47 America/Los_Angeles, receipt_creation_date_pst -> 2016-12-05 01:44:41 America/Los_Angeles, adam_id -> 0,
    //      in_app -> List(Map(purchase_date -> 2016-12-05 09:44:41 Etc/GMT, quantity -> 1, original_purchase_date -> 2016-12-05 09:44:41 Etc/GMT, original_purchase_date_ms -> 1480931081000, purchase_date_pst -> 2016-12-05 01:44:41 America/Los_Angeles, purchase_date_ms -> 1480931081000, original_purchase_date_pst -> 2016-12-05 01:44:41 America/Los_Angeles, is_trial_period -> false, transaction_id -> 1000000256159353, original_transaction_id -> 1000000256159353, product_id -> com.dz.6)), original_application_version -> 1.0)).toJson())
    //      println("900000000".encrypt())
    //      println(Int.MaxValue/100)
    //
    //      val ru = scala.reflect.runtime.universe
    //      val person=new Person(1,"狗屎")
    ////
    //      val m = universe.runtimeMirror(getClass.getClassLoader)
    //
    //      val k=universe.typeOf[Key].typeSymbol.asClass
    ////      val t=universe.typeOf[Person]
    //
    //      val ptype=universe.internal.manifestToTypeTag(m, ManifestFactory.classType(person.getClass))
    //
    ////
    //      val t=ptype.tpe.asInstanceOf[reflect.runtime.universe.Type]
    //      val classPerson = t.typeSymbol.asClass//.asInstanceOf[reflect.runtime.universe.ClassSymbol]
    //      val cm = m.reflectClass(classPerson)
    ////      val ctor = t.decl(universe.termNames.CONSTRUCTOR).asMethod
    //      val cmethod=t.decls.filter(_.annotations.map(_.tree.tpe.typeSymbol.asClass).contains(k))
    ////      val ctorm = cm.reflectConstructor(ctor)
    ////      val p = ctorm(1, "Mike")
    //      cmethod.foreach(v=>println(v.name.toString))
    ////      println(p)
    ////
    //      new LiveRoomUser().getPrimaryKeys.foreach(v=>println(v))
//    new UserMessage().createTable()
//    Thread.sleep(1000*100)
//    System.exit(0)
  }

}

