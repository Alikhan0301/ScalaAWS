package week10.actors



import java.io.File

import akka.actor.{Actor, ActorLogging, Props, ReceiveTimeout}
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import com.amazonaws.AmazonServiceException
import week10.models.{Path, Response}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{GetObjectRequest, PutObjectResult}
import com.amazonaws.services.s3.model.{GetObjectRequest, ListObjectsRequest, ObjectListing, PutObjectResult}
import week10.models.ErrorInfo

object ExtendedService{
  def props(s3Client: AmazonS3, prefixPathOut: String, prefixPathIn: String, bucketName: String) = Props(new ExtendedService(s3Client, prefixPathOut, prefixPathIn, bucketName))

  case object Download

  case object Upload
}
class ExtendedService(s3Client: AmazonS3, prefixPathOut: String, prefixPathIn: String, bucketName: String) extends Actor with ActorLogging {

  import ExtendedService._

  override def preStart(): Unit = {
    log.info("ExtendedService actor started!")
  }

  override def receive: Receive = {
    case Download =>
      val listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName)
      val list: ObjectListing = s3Client.listObjects(bucketName)
      list.getObjectSummaries().forEach(key => downloadFromAws(key.getKey()))
      sender() ! Right(Response(200, "All the files from s3 are downloaded to in"))
    case Upload =>
      log.info("Upload from out request")
      dfs_upload("", new File(prefixPathOut))
      sender() ! Right(Response(200, "All the files from out are uploaded to s3"))
    case ReceiveTimeout =>
      sender() ! Left(ErrorInfo(444, "Timeout received"))
  }

  def dfs_upload(curPath: String, file: File): Unit = {
    val wholePath = prefixPathOut + curPath
    if (!s3Client.doesObjectExist(bucketName, curPath) && curPath != "" && file.isFile)
      s3Client.putObject(bucketName, curPath, file)

    if (file.isDirectory) {
      file.listFiles.foreach(to_file => {
        val suff = to_file.getPath().substring(to_file.getPath.lastIndexOf('/') + 1)
        dfs_upload(curPath + "/" + suff, to_file)
      })
    }
  }

  def downloadFromAws(path: String): Unit = {
    val wholePath = (prefixPathIn + path).substring(0, (prefixPathIn + path).lastIndexOf('/'))
    val newDir = new File(wholePath)
    newDir.mkdir()
    s3Client.getObject(new GetObjectRequest(bucketName, path),
      new File(prefixPathIn + path))
  }
}
