package week10


import akka.http.scaladsl.server.Directives.{as, complete, concat, entity, get, parameters, path, post}
import akka.util.Timeout
import week10.actors.{AwsService, ExtendedService}
import week10.models.{ErrorInfo, Response, Path => PathModel}

import scala.concurrent.Future
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.util.Timeout
import akka.http.scaladsl.marshalling.ToResponseMarshallable

import scala.concurrent.duration._


trait AwsRoutes extends JsonSupporter {
  def awsActor: ActorRef
  def extendedService: ActorRef

  implicit val timeout = Timeout(30.seconds)


  val task1Routes = path("file") {
    // HTTP POST
    concat(
      post {
        entity(as[PathModel]) { pathModel =>
          complete {
            (awsActor ? AwsService.Upload(pathModel.path)).mapTo[Either[ErrorInfo, Response]]
          }
        }
      },
      get {
        parameters("path".as[String]) { path =>
          complete{
            (awsActor ? AwsService.Download(path)).mapTo[Either[ErrorInfo, Response]]
          }
        }
      },
      delete {
        parameters("path".as[String]) { path =>
          complete {
            (awsActor ? AwsService.Delete(path)).mapTo[Either[ErrorInfo, Response]]
          }
        }
      }
    )
  }
  val task2RoutesOut = path("out") {
    get{
      complete{
        (extendedService ? ExtendedService.Upload).mapTo[Either[ErrorInfo, Response]]
      }
    }
  }
  val task2RoutesIn = path("in") {
    get{
      complete{
        (extendedService ? ExtendedService.Download).mapTo[Either[ErrorInfo, Response]]
      }
    }
  }
}
