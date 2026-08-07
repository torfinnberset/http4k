package org.http4k.connect.amazon.iotdataplane.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class TopicName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<TopicName>(::TopicName)
}
