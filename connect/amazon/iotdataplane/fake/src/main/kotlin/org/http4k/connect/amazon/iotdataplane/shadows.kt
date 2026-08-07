package org.http4k.connect.amazon.iotdataplane

import org.http4k.connect.amazon.iotdataplane.model.ShadowName
import org.http4k.connect.amazon.iotdataplane.model.ThingName
import org.http4k.core.Request
import org.http4k.format.MoshiInteger
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiNull
import org.http4k.format.MoshiObject
import org.http4k.routing.path

private const val STATE = "state"
private const val VERSION = "version"

fun storageKey(thingName: ThingName, shadowName: ShadowName? = null) =
    "${thingName.value}/${shadowName?.value.orEmpty()}"

/** Recursively applies an AWS shadow update. */
internal fun merge(existing: MoshiNode?, delta: MoshiNode): MoshiNode = when {
    existing is MoshiObject && delta is MoshiObject -> MoshiObject(
        (existing.attributes + delta.attributes)
            .filterValues { it != MoshiNull }
            .mapValues { (name, value) -> merge(existing[name], value) }
            .toMutableMap()
    )

    else -> delta
}

internal fun updatedShadow(existing: MoshiNode?, delta: MoshiNode) = MoshiObject(
    STATE to merge(existing.attribute(STATE), delta.state()),
    VERSION to MoshiInteger(existing.version() + 1)
)

internal fun acceptedShadow(delta: MoshiNode, version: Int) = MoshiObject(
    STATE to delta.state(),
    VERSION to MoshiInteger(version)
)

internal fun deletedShadow(version: Int) = MoshiObject(VERSION to MoshiInteger(version))

internal fun MoshiNode?.version() = (attribute(VERSION) as? MoshiInteger)?.value ?: 0

private fun MoshiNode?.state() = attribute(STATE) ?: MoshiObject()

private fun MoshiNode?.attribute(name: String) = (this as? MoshiObject)?.get(name)

internal fun Request.thingName() = ThingName.of(path("thingName")!!)

internal fun Request.shadowName() = query("name")?.let(ShadowName::of)

internal fun Request.storageKey() = storageKey(thingName(), shadowName())

internal fun Request.shadowNotFound() =
    resourceNotFound("No shadow exists with name: '${shadowName()?.value ?: "classic"}'")
