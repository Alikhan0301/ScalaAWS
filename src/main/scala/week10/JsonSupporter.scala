package week10

import spray.json.DefaultJsonProtocol.jsonFormat4
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.RootJsonFormat
import week10.models
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat


trait JsonSupporter {
  implicit val pathModelFormat: RootJsonFormat[models.Path] = jsonFormat1(models.Path)
  implicit val errorInfoFormat = jsonFormat2(models.ErrorInfo)
  implicit val responseFormat = jsonFormat2(models.Response)
}
