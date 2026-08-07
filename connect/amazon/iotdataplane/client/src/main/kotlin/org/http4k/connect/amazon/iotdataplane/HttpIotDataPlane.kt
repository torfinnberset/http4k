package org.http4k.connect.amazon.iotdataplane

import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.connect.amazon.AWS_REGION
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.Environment
import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.Payload.Mode.Signed
import java.time.Clock

/** HTTP client for an account-specific IoT data endpoint. */
fun IotDataPlane.Companion.Http(
    endpoint: Uri,
    region: Region,
    credentialsProvider: CredentialsProvider,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
) = object : IotDataPlane {
    private val signedHttp = signAwsRequests(region, credentialsProvider, clock, Signed, endpoint).then(http)

    override fun <R : Any> invoke(action: IotDataPlaneAction<R>) = action.toResult(signedHttp(action.toRequest()))
}

/** Creates a client from system environment variables. */
fun IotDataPlane.Companion.Http(
    endpoint: Uri,
    env: Map<String, String> = System.getenv(),
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env),
) = Http(endpoint, Environment.from(env), http, clock, credentialsProvider)

/** Creates a client from an http4k [Environment]. */
fun IotDataPlane.Companion.Http(
    endpoint: Uri,
    env: Environment,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env),
) = Http(endpoint, AWS_REGION(env), credentialsProvider, http, clock)
