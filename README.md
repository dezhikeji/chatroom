


项目说明
=====================

1. 此项目用于聊天室

2. 部署构建
    1. 使用sbt dist 将target\universal 下的压缩包拷贝解压即可
    2. 执行压缩包下的bin/项目名  即可启动
    3. 默认启动端口9000   启动加上 -Dhttp.port=80 来改变端口号
    4. cookie session技术,可以按照房间动态增减服务器
    

3. 项目所用的技术
    1. [play](https://playframework.com/documentation/2.5.x/ScalaHome)  2.5.x-scala   使用技术 gzip\template\async
    2. [scala](http://zh.scala-tour.com/) 2.11.8          使用技术 actor\functions\trait\currying\partialfunctions\implicit\Future
    3. [zxorm](http://git.oschina.net/livehl/zxorm) 2.1.0           mysql
    4. [阿里](http://www.aliyun.com)中间件             ots
    5. 其他依赖               httpclient  javassist  jackson   dbutils

4. 代码结构
    1. app           具体实现逻辑
    2. common        加解密\时间\常用工具类(缓存\MD5\异步执行\魔法方法\压缩解压)
    3. controllers   具体http方法   BaseController提供帮助的父类 UtilController  工具类   AdminController 具体逻辑类
    4. db            数据库ORM  sql
    5. bdb            数据库ORM  nosql
    6. tools         各种加密类  各种加解密  各种平台的工具类
    7. Global        play全局设置文件,用于启动初始化以及启动配置
    8. actors         各种异步actor

5. 相关图表
    1.暂无

6. 部署环境
    1.docker   bin/poster -J-Xms500M -J-Xmx950m -J-server -J-javaagent:/opt/docker/OneAPM/oneapm.jar
    2.server





7. 部署流程(默认阿里云)
   1.概述
        1.本系统整体架构基于大规模、超大规模访问量设计，需要分布式运行，每个模块至少保持三副本运行，使用docker进行版本切换和部署。
   2.系统架构图
        1.https://cacoo.com/diagrams/yq46rceIiLFSbFUq

   3.部署细节
        1.基础环境准备
            1.配置分布式数据库,配置底层数据库节点，设置编码为UTF-8，导入测试环境的表结构，并且设置分表
            2.配置Nosql数据库，创建数据表
            3.购买物理机，并配置好docker管理环境，推荐使用阿里云容器服务
        2.初始化
            1.设置docker应用配置环境变量和参数
                1.DB_USER   分布式数据库用户名
                2.DB_PWD    分布式数据库密码
                3.DB_URl    分布式数据库地址
                5.OTS_NAME   Nosql数据库表名
                6.OTS_URL    Nosql数据库链接地址
                7.OTS_ID     Nosql认证用户名
                8.OTS_KEY    Nosql认证密码
                9.配置健康检查
                10.配置自动重启
                11.配置平滑升级
                12.配置系统域名
            2.准备数据
                1.其他必要数据
            3.重启
        3.性能压测
            1.测试单应用主要接口性能
            2.测试集群主要接口性能
            3.增加集群节点，测试主要接口数据库压力
            4.增加集群节点，测试主要接口nosql压力

         7.清理测试、压测数据，正式上线运行





