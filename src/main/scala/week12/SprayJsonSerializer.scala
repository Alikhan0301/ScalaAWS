package week12

import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import week12.models.{ErrorResponse, SuccessfulResponse}

trait SprayJsonSerializer extends DefaultJsonProtocol {

  implicit val successfulFormat: RootJsonFormat[SuccessfulResponse] = jsonFormat2(SuccessfulResponse)
  implicit val errorFormat: RootJsonFormat[ErrorResponse] = jsonFormat2(ErrorResponse)
}