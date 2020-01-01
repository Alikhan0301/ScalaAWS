package week10.actors



import java.io.File

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import com.amazonaws.AmazonServiceException
import week10.models.{Path, Response}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{GetObjectRequest, PutObjectResult}
import week10.models.ErrorInfo

object AwsService{

  def props(s3Client: AmazonS3, prefixPath: String, bucketName: String) = Props(new AwsService(s3Client, prefixPath, bucketName))

  case class Download(path: String)

  case class Upload(path: String)

  case class Delete(path: String)

  case class Update(path: String, new_path: String)
}
class AwsService(s3Client: AmazonS3, prefixPath: String, bucketName: String) extends Actor with ActorLogging {

  import AwsService._

  override def preStart(): Unit = {
    log.info("AwsService actor started!")
  }

  override def receive: Receive = {
    case Download(path) =>
      log.info(s"Download request with path: $path")
      if(s3Client.doesObjectExist(bucketName, path)){
        downloadFromAws(path)
        sender() ! Right(Response(200, "File downloaded from aws"))
      } else{
        sender() ! Left(ErrorInfo(404, "File not found"))
      }
    case Upload(path) =>
      log.info(s"Upload file request with path: $path")
      val file = new File(prefixPath + path)
      if(!file.exists())
        sender() ! Left(ErrorInfo(404, "File not found"))
      else{
        uploadToAws(path)
        sender() ! Right(Response(200, "File uploaded to aws"))
      }
    case Delete(path) =>
      if(s3Client.doesObjectExist(bucketName, path)){
        deleteFromAws(path)
        sender() ! Right(Response(200, "File deleted from s3"))
      } else{
        sender() !  Left(ErrorInfo(404, "File not found"))
      }
  }

  def deleteFromAws(path: String): Unit = {
    s3Client.deleteObject(bucketName, path)
  }
  def downloadFromAws(path: String): Unit = {
    val wholePath = (prefixPath + path).substring(0, (prefixPath + path).lastIndexOf('/'))
    val newDir = new File(wholePath)
    newDir.mkdir()
    s3Client.getObject(new GetObjectRequest(bucketName, path),
      new File(prefixPath + path))
  }
  def uploadToAws(path: String): Unit = {
    s3Client.putObject(bucketName, path, new File(prefixPath + path))
  }
}
