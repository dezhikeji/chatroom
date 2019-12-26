import com.typesafe.sbt.packager.docker._
import sbt.Keys._

name := "chatroom"

version := "2.5.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  filters,
  "com.typesafe.akka" %% "akka-remote" % "2.4.10",
  "org.apache.httpcomponents" % "httpmime" % "4.5.2",
  "org.apache.httpcomponents" % "httpclient" % "4.5.2",
  "mysql" % "mysql-connector-java" % "5.1.39",
  "commons-dbutils" % "commons-dbutils" % "1.6",
  "org.javassist" % "javassist" % "3.20.0-GA",
  "com.aliyun.openservices" % "tablestore" % "4.1.0",
  "com.alibaba" % "druid" % "1.0.24",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.7.5",
  "net.jpountz.lz4" % "lz4" % "1.3.0",
  "io.swagger" %% "swagger-play2" % "1.5.2",
  "com.twitter" %% "chill-akka" % "0.9.2",
  specs2 % Test
)

val root = (project in file(".")).enablePlugins(PlayScala).enablePlugins(DockerPlugin).enablePlugins(JavaAppPackaging)

javaOptions in Test ++= Seq(
  "-Dconfig.file=conf/debug.conf"
)

doc in Compile <<= target.map(_ / "none")

TwirlKeys.templateImports += "common.Tool._"

javaOptions in Universal ++= Seq(
  "-Dpidfile.path=/dev/null","-Dfile.encoding=utf-8"
)

dockerCommands := Seq(
  Cmd("FROM", "livehl/java8"),
  Cmd("WORKDIR", "/opt/docker"),
  ExecCmd("copy", "opt/", "/opt/"),
  Cmd("EXPOSE", "9000"),
  ExecCmd("CMD", "bin/"+name.value)
)

dockerExposedPorts := Seq(9000, 2552)

packageName in Docker := packageName.value

dockerUpdateLatest in Docker := true

dockerRepository := Some("registry.cn-hangzhou.aliyuncs.com/cdhub")
