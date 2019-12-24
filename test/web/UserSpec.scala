package web

import org.junit.runner._
import org.specs2.runner._
import play.api.test.Helpers._
import play.api.test._
import common.Tool._

import scala.util.Random

/**
* Add your spec here.
* You can mock out a whole application including requests, plugins etc.
* For more information, consult the wiki.
*/
@RunWith(classOf[JUnitRunner])
class UserSpec extends BaseSpec {

  "web.UserSpec" should {

    "User" in new WithApplication {
      //      addProductNeed
      //      addCart
      //      cart
    }

  }

  def logIn = {
    val login = route(FakeRequest(GET, "/api/user/a1c07e25d61b4a1d9db24c1ee30ec627")).get
    get(login)
    cookies(login)(timeout) toList
  }

  def bind = {
    val cookie = logIn
    val data = List(("account", "livehl@126.com"), ("pwd", "890218"))
    val bind = route(FakeRequest(POST, "/api/user/bind").withFormUrlEncodedBody(data: _*).withCookies(cookie: _*)).get
    get(bind)
  }

  def updatePwd: Unit = {
    val cookie = logIn
    val data = List(("newPwd", "890218"), ("pwd", "19890218"))
    val pwd = route(FakeRequest(POST, "/api/user/updatePwd").withFormUrlEncodedBody(data: _*).withCookies(cookie: _*)).get
    get(pwd)
  }
}
