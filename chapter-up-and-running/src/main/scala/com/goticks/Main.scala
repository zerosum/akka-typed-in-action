package com.goticks

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object Main extends App with RequestTimeout {

  val config = ConfigFactory.load()
  val host   = config.getString("http.host") // 設定からホスト名とポートを取得
  val port   = config.getInt("http.port")

  val log = LoggerFactory.getLogger("hoge")

  ActorSystem[Done](
    Behaviors.setup { ctx =>
      implicit val system: ActorSystem[Nothing] = ctx.system
      implicit val ec: ExecutionContextExecutor = system.executionContext // bindAndHandleは暗黙のExecutionContextが必要

      val api = RestApi(ctx, requestTimeout(config)).routes // the RestApi provides a Route

      val bindingFuture: Future[ServerBinding] = {
        Http().newServerAt(host, port).bind(api)
      } // HTTPサーバーの起動

      bindingFuture
        .map { serverBinding =>
          log.info(s"RestApi bound to ${serverBinding.localAddress} ")
        }
        .onComplete {
          case Success(_) =>
            log.info(s"Success to bind to $host:$port")
          case Failure(ex) =>
            log.error(s"Failed to bind to $host:$port!", ex)
            system.terminate()
        }

      Behaviors.receiveMessage { case Done =>
        Behaviors.stopped
      }
    },
    "restApi"
  )
}

trait RequestTimeout {
  import scala.concurrent.duration._
  def requestTimeout(config: Config): Timeout = {
    val t = config.getString("akka.http.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}
