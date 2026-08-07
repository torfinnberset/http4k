import dev.forkhandles.result4k.Result
import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.iotdataplane.FakeIotDataPlane
import org.http4k.connect.amazon.iotdataplane.Http
import org.http4k.connect.amazon.iotdataplane.IotDataPlane
import org.http4k.connect.amazon.iotdataplane.model.PayloadFormatIndicator.UTF8_DATA
import org.http4k.connect.amazon.iotdataplane.model.TopicName
import org.http4k.connect.amazon.iotdataplane.publish
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.filter.debug

const val USE_REAL_CLIENT = false

fun main() {
    val region = Region.of("us-east-1")
    val topic = TopicName.of("http4k/example/topic")

    // IoT data endpoints are account-specific.
    val endpoint = Uri.of("https://000000000-ats.iot.us-east-1.amazonaws.com")

    val http: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else FakeIotDataPlane()

    val client = IotDataPlane.Http(endpoint, region, { AwsCredentials("accessKeyId", "secretKey") }, http.debug())

    val published: Result<Unit, RemoteFailure> = client.publish(topic, """{"message":"hello"}""".toByteArray())
    println(published)

    println(
        client.publish(
            topic = topic,
            payload = """{"message":"hello again"}""".toByteArray(),
            qos = 1,
            contentType = "application/json",
            payloadFormatIndicator = UTF8_DATA,
            userProperties = listOf("source" to "http4k")
        )
    )
}
