package week10

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{concat, pathPrefix}
import akka.stream.ActorMaterializer
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.Bucket
import org.slf4j.LoggerFactory
import week10.actors.{AwsService, ExtendedService}

import collection.JavaConverters._

object Boot extends App with AwsRoutes {

  val log = LoggerFactory.getLogger("Boot")

  val awsCreds = new BasicAWSCredentials(
    "something",
    "something")

  // Frankfurt client
  val s3Client: AmazonS3 = AmazonS3ClientBuilder.standard
    .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
    .withRegion(Regions.EU_CENTRAL_1)
    .build

  // check if bucket exists

  if (s3Client.doesBucketExistV2("bhle-image-bucket")) {
    log.info("Bucket exists")
  }

  // get all objects

  val buckets = s3Client.listBuckets().asScala.toList
  buckets.foreach(b => log.info(s"Bucket: ${b.getName}"))

  // create a bucket
  if (s3Client.doesBucketExistV2("bhle-file-bucket")) {
    log.info("bhle-file-bucket exists")
  } else {
    s3Client.createBucket("bhle-file-bucket")
  }

  // Add file
  val file = new File("src/main/resources/poem.txt")
  log.info(s"Putting file: ${file.getAbsolutePath}")
  s3Client.putObject("task111-bucket", file.getName, file)


  // TASK1 and TASK2

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // needed for the future map/flatmap in the end and future in fetchItem and saveOrder
  implicit val executionContext = system.dispatcher

  val prefixPath = "src/main/resources/s3/"
  val prefixPathOut= "src/main/resources/out/"
  val prefixPathIn = "src/main/resources/in/"

  val bucketName = "task111-bucket"
  val bucketName2 = "task222-bucket"

  val awsActor = system.actorOf(AwsService.props(s3Client, prefixPath, bucketName))
  val extendedService = system.actorOf(ExtendedService.props(s3Client, prefixPathOut, prefixPathIn, bucketName2))


  val route =
    pathPrefix("api") {
      concat(
        task1Routes,
        task2RoutesOut,
        task2RoutesIn,
      )
    }
  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
  log.info("Listening on port 8080...")
}
