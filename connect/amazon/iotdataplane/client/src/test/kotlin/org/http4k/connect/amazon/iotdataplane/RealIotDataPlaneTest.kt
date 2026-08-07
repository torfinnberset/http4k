package org.http4k.connect.amazon.iotdataplane

import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.RealAwsContract
import org.http4k.core.Uri
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.lang.System.getenv

/** Runs against `HTTP4K_IOT_DATA_PLANE_ENDPOINT` when set. */
class RealIotDataPlaneTest : IotDataPlaneContract, RealAwsContract {
    override val http = JavaHttpClient()

    override val endpoint: Uri
        get() = getenv("HTTP4K_IOT_DATA_PLANE_ENDPOINT")
            .also { assumeTrue(it != null, "HTTP4K_IOT_DATA_PLANE_ENDPOINT not set") }
            .let(Uri::of)
}
