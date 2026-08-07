package org.http4k.connect.amazon.iotdataplane

import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND

internal fun resourceNotFound(message: String) = Response(NOT_FOUND)
    .header("x-amzn-ErrorType", "ResourceNotFoundException")
    .body(IotDataPlaneMoshi.asFormatString(mapOf("message" to message)))
