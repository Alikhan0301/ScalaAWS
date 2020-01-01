package week12.actors

import java.io.{File, InputStream}

import akka.actor.{Actor, ActorLogging, Props}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{DeleteObjectRequest, GetObjectRequest, ObjectMetadata, PutObjectRequest, S3ObjectInputStream}
import com.amazonaws.util.IOUtils
import week12.models.{ErrorResponse, PhotoResponse, SuccessfulResponse}

object PhotoActor {

  case class UploadPhoto(inputStream: InputStream, fileName: String, contentType: String)

  case class GetPhoto(fileName: String)

  def props(client: AmazonS3, bucketName: String) = Props(new PhotoActor(client, bucketName))
}

class PhotoActor(client: AmazonS3, bucketName: String) extends Actor with ActorLogging {

  import PhotoActor._

  override def receive: Receive = {
    case UploadPhoto(inputStream, fileName, contentType) =>
      // Upload a file as a new object with ContentType and title specified.
      val rootSender = sender()
      val objectKey = s"photos/${fileName}"

      if (client.doesObjectExist(bucketName, objectKey)) {
        rootSender ! Left(ErrorResponse(409, s"Photo with FILENAME: ${fileName} already exists"))
        log.info(s"Failed to upload photo with FILENAME: ${fileName}. It already exists")
      } else {
        val metadata = new ObjectMetadata()
        metadata.setContentType(contentType)
        val request = new PutObjectRequest(bucketName, objectKey, inputStream, metadata)
        val result = client.putObject(request)

        rootSender ! Right(SuccessfulResponse(201, s"File version: ${result.getVersionId}"))
        log.info("Successfully put photo-object with FILENAME: {} to AWS S3", fileName)
      }

//      context.stop(self)

    case GetPhoto(fileName) =>
      val rootSender = sender()
      val objectKey = s"photos/$fileName"

      if (client.doesObjectExist(bucketName, objectKey)) {
        val x = client.getObject(new GetObjectRequest(bucketName, objectKey)).getObjectMetadata.getContentType
        val fullObject: S3ObjectInputStream = client.getObject(new GetObjectRequest(bucketName, objectKey)).getObjectContent
        val photoInBytes: Array[Byte] = IOUtils.toByteArray(fullObject)

        log.info(s"Status: 200. Successfully found photo with FILENAME: ${fileName}")
        rootSender ! Right(PhotoResponse(200, photoInBytes))
      } else {
        log.info(s"Status: 404. Failed to get photo with FILENAME: ${fileName}. It doesn't exist")
        rootSender ! Left(ErrorResponse(404, s"Failed to get photo with FILENAME: ${fileName}. It doesn't exist"))
      }

//      context.stop(self)
  }

}