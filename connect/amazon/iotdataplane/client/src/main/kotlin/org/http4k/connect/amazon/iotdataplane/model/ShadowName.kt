package org.http4k.connect.amazon.iotdataplane.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.and
import dev.forkhandles.values.maxLength
import dev.forkhandles.values.regex

class ShadowName private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<ShadowName>(::ShadowName, 64.maxLength.and("[a-zA-Z0-9:_-]+".regex))
}
