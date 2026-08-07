package org.http4k.connect.amazon.iotdataplane

import org.http4k.connect.amazon.iotdataplane.model.ClientId
import org.http4k.core.Request
import org.http4k.routing.path

internal fun Request.clientId() = ClientId.of(path("clientId")!!)

internal fun connectionNotFound(clientId: ClientId) =
    resourceNotFound("No connection found for client: '${clientId.value}'")
