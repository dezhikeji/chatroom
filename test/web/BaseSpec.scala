package web

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import common.AESCoder
import org.specs2.mutable._
import play.api.http.HttpVerbs
import play.api.test.FakeRequest
import play.api.test.Helpers._
import common.Tool._

import scala.concurrent.duration._
import scala.util.parsing.json._


/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class BaseSpec extends Specification {
//  val fakeApplicationWithGlobal = FakeApplication(withGlobal = Some(new GlobalSettings() {
//    override def onStart(app: Application) { println("Hello world!") }
//  }))

  implicit val timeout = Timeout(Duration.apply(600,TimeUnit.SECONDS))

  def get(of : scala.concurrent.Future[play.api.mvc.Result])={
    val respone=new String(contentAsBytes(of)(timeout).toArray,"utf-8")
    println(respone)
    val code=status(of)(timeout)
    if(code==404) throw new Exception("路径不存在")
    else if(code !=200) throw new Exception(code+"错误:"+respone)
    respone.jsonToMap.asInstanceOf[Map[String,Any]]
  }
  def getBean(of : scala.concurrent.Future[play.api.mvc.Result])={
    val json=get(of)
    json.get("bean").get.asInstanceOf[Map[String,Any]]
  }
  def getList(of : scala.concurrent.Future[play.api.mvc.Result])={
    val json=get(of)
    json.get("list").get.asInstanceOf[List[Any]]
  }

  def checkBean(data:Map[String,Any],keys:String*)={
     def chekKey(d:Map[String,Any],key:String){
      if(d.get(key).isEmpty){
        throw new Exception("字段缺失:"+key)
      }
    }
    keys foreach(chekKey(data,_))
  }
  def checkList(list:List[Any],keys:String*)={
    list foreach (b=>checkBean(b.asInstanceOf[Map[String,Any]],keys:_*))
  }
  def sign(request:String,time:String,random:String)={
    AESCoder.encrypt((request+time+random),"cfhuydhsfckas")
  }
  def safeRequest(method:String,path:String)={
    val time=System.currentTimeMillis().toString
    val nonce=randomStr(10)
    val header=Map("time"->time,"nonce"->nonce,"sign"->sign(path,time,nonce))
    route(FakeRequest(method, path).withHeaders(header.toList:_*))
  }
}
