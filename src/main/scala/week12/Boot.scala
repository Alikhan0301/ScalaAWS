package week12

import java.io.InputStream
import java.util.concurrent.TimeUnit

import week12.actors.PhotoActor
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import akka.stream.scaladsl.StreamConverters
import akka.util.Timeout
import org.slf4j.LoggerFactory
import akka.http.scaladsl.model.{ContentType, HttpEntity}
import akka.http.scaladsl.model.MediaTypes.`image/png`
import akka.http.scaladsl.server.RequestContext
import week12.SprayJsonSerializer
import week12.models.{ErrorResponse, PhotoResponse, SuccessfulResponse}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._

object Boot extends App with SprayJsonSerializer {

  implicit val system: ActorSystem = ActorSystem("photo-service")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit val timeout: Timeout = Timeout(10.seconds)

  val log = LoggerFactory.getLogger("Boot")

  val client: AmazonS3 = AmazonS3Service.client
  val bucketName = "photo-service-kbtu123"
  AmazonS3Service.createBucket(bucketName)

  val worker = system.actorOf(PhotoActor.props(client, bucketName))

  val route =
    path("health") {
      get {
        complete {
          "OK"
        }
      }
    } ~
      pathPrefix("photos") {
        concat(
          pathEndOrSingleSlash {
            concat(
              get {
                complete {
                  "all photos"
                }
              },
              post {
                extractRequestContext { ctx: RequestContext =>
                  fileUpload("file") {
                    case (metadata, byteSource) =>
                      // Convert Source[ByteString, Any] => InputStream
                      val inputStream: InputStream = byteSource.runWith(
                        StreamConverters.asInputStream(FiniteDuration(3, TimeUnit.SECONDS))
                      )

                      log.info(s"Content type: ${metadata.contentType}")
                      log.info(s"Field name: ${metadata.fieldName}")
                      log.info(s"File name: ${metadata.fileName}")

                      handle((worker ? PhotoActor.UploadPhoto(inputStream, metadata.fileName, metadata.contentType.toString())).mapTo[Either[ErrorResponse, SuccessfulResponse]])
                  }
                }
              }
            )
          },
          path(Segment) { photoName =>
            concat(
              get {
                val future = (worker ? PhotoActor.GetPhoto(photoName)).mapTo[Either[ErrorResponse, PhotoResponse]]

                onSuccess(future) {
                  case Left(error) => complete(error.status, error.message)
                  case Right(photo) => complete(photo.status, HttpEntity(ContentType(`image/png`), photo.message))
                }
              }
            )
          }
        )
      }


  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)

  def handle(output: Future[Either[ErrorResponse, SuccessfulResponse]]) = {
    onSuccess(output) {
      case Left(error) => {
        complete(error.status, error)
      }
      case Right(successful) => {
        complete(successful.status, successful)
      }
    }
  }
}