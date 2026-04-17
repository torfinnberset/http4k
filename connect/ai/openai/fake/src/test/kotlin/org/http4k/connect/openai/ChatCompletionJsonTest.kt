package org.http4k.connect.openai

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import com.natpryce.hamkrest.present
import org.http4k.ai.model.ApiKey
import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.ModelName
import org.http4k.connect.openai.OpenAIMoshi.autoBody
import org.http4k.connect.openai.action.ChatCompletion
import org.http4k.connect.openai.action.CompletionResponse
import org.http4k.connect.openai.action.JsonSchemaSpec
import org.http4k.connect.openai.action.Message
import org.http4k.connect.openai.action.ResponseFormat
import org.http4k.connect.openai.action.TopLogProb
import org.http4k.connect.successValue
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.format.MoshiBoolean
import org.http4k.format.MoshiObject
import org.http4k.format.MoshiString
import org.junit.jupiter.api.Test

class ChatCompletionJsonTest {

    private val responseLens = autoBody<CompletionResponse>().toLens()
    private val requestLens = autoBody<ChatCompletion>().toLens()

    private fun ChatCompletion.roundTrip(): ChatCompletion =
        requestLens(toRequest())

    // --- Request serialization round-trips ---

    @Test
    fun `logprobs fields round-trip through serialization`() {
        val original = ChatCompletion(
            model = ModelName.of("gpt-4"),
            messages = listOf(Message.User("hello")),
            max_tokens = MaxTokens.of(100),
            logprobs = true,
            top_logprobs = 5
        )
        val parsed = original.roundTrip()

        assertThat(parsed.logprobs, equalTo(true))
        assertThat(parsed.top_logprobs, equalTo(5))
    }

    @Test
    fun `null optional fields are absent after round-trip`() {
        val original = ChatCompletion(
            model = ModelName.of("gpt-4"),
            messages = listOf(Message.User("hello")),
            max_tokens = MaxTokens.of(100)
        )
        val parsed = original.roundTrip()

        assertThat(parsed.logprobs, absent())
        assertThat(parsed.top_logprobs, absent())
    }

    @Test
    fun `response_format json_schema serializes strict inside json_schema not alongside it`() {
        val request = ChatCompletion(
            model = ModelName.of("gpt-4"),
            messages = listOf(Message.User("classify")),
            max_tokens = MaxTokens.of(100),
            response_format = ResponseFormat.JsonSchema(
                JsonSchemaSpec(
                    name = "classification",
                    schema = mapOf(
                        "type" to "object",
                        "required" to listOf("result"),
                        "properties" to mapOf("result" to mapOf("type" to "string"))
                    ),
                    strict = true
                )
            )
        )

        // Verify JSON structure matches OpenAI spec: strict must be INSIDE json_schema, not alongside it
        val json = OpenAIMoshi.parse(request.toRequest().bodyString()) as MoshiObject
        val responseFormat = json["response_format"] as MoshiObject
        assertThat(responseFormat["type"], equalTo(MoshiString("json_schema") as Any))
        assertThat(responseFormat.attributes.containsKey("strict"), equalTo(false))

        val jsonSchemaObj = responseFormat["json_schema"] as MoshiObject
        assertThat(jsonSchemaObj["name"], equalTo(MoshiString("classification") as Any))
        assertThat(jsonSchemaObj["strict"], equalTo(MoshiBoolean(true) as Any))
        assertThat((jsonSchemaObj["schema"] as MoshiObject)["type"], equalTo(MoshiString("object") as Any))

        // Also verify round-trip back to typed objects
        val parsed = request.roundTrip()
        val jsonSchema = parsed.response_format as ResponseFormat.JsonSchema
        assertThat(jsonSchema.json_schema.name, equalTo("classification"))
        assertThat(jsonSchema.json_schema.strict, equalTo(true))
        assertThat(jsonSchema.json_schema.schema["type"], equalTo("object" as Any))
    }

    // --- Response deserialization ---

    @Test
    fun `response deserializes logprobs structure`() {
        val response = responseLens(Response(OK).body("""
        {
            "id": "chatcmpl-123",
            "object": "chat.completion",
            "created": 1700000000,
            "model": "gpt-4",
            "choices": [{
                "index": 0,
                "message": { "role": "assistant", "content": "NONE" },
                "finish_reason": "stop",
                "logprobs": {
                    "content": [{
                        "token": "NONE",
                        "logprob": -0.001,
                        "bytes": [78, 79, 78, 69],
                        "top_logprobs": [
                            {"token": "NONE", "logprob": -0.001, "bytes": [78, 79, 78, 69]},
                            {"token": "TEXT", "logprob": -7.52, "bytes": [84, 69, 88, 84]}
                        ]
                    }]
                }
            }],
            "usage": {"prompt_tokens": 10, "completion_tokens": 1, "total_tokens": 11}
        }
        """))

        val logprobs = response.choices[0].logprobs!!
        val tokenLogProb = logprobs.content!![0]
        assertThat(tokenLogProb.token, equalTo("NONE"))
        assertThat(tokenLogProb.logprob, equalTo(-0.001))
        assertThat(tokenLogProb.bytes, equalTo(listOf(78, 79, 78, 69)))
        assertThat(
            tokenLogProb.top_logprobs!![0],
            equalTo(TopLogProb("NONE", -0.001, listOf(78, 79, 78, 69)))
        )
        assertThat(
            tokenLogProb.top_logprobs!![1],
            equalTo(TopLogProb("TEXT", -7.52, listOf(84, 69, 88, 84)))
        )
    }

    @Test
    fun `response deserializes reasoning field`() {
        val response = responseLens(Response(OK).body("""
        {
            "id": "chatcmpl-456",
            "object": "chat.completion",
            "created": 1700000000,
            "model": "gpt-oss-20b",
            "choices": [{
                "index": 0,
                "message": {
                    "role": "assistant",
                    "content": "PHONE",
                    "reasoning": "The note says per pt which indicates phone."
                },
                "finish_reason": "stop"
            }]
        }
        """))

        assertThat(response.choices[0].message.reasoning, equalTo("The note says per pt which indicates phone."))
    }

    @Test
    fun `response without logprobs or reasoning deserializes with nulls`() {
        val response = responseLens(Response(OK).body("""
        {
            "id": "chatcmpl-000",
            "object": "chat.completion",
            "created": 1700000000,
            "model": "gpt-4",
            "choices": [{
                "index": 0,
                "message": { "role": "assistant", "content": "Hello!" },
                "finish_reason": "stop"
            }]
        }
        """))

        assertThat(response.choices[0].logprobs, absent())
        assertThat(response.choices[0].message.reasoning, absent())
        assertThat(response.choices[0].message.reasoning_content, absent())
    }

    @Test
    fun `response deserializes reasoning_content field (DeepSeek)`() {
        val response = responseLens(Response(OK).body("""
        {
            "id": "chatcmpl-789",
            "object": "chat.completion",
            "created": 1700000000,
            "model": "deepseek-reasoner",
            "choices": [{
                "index": 0,
                "message": {
                    "role": "assistant",
                    "content": "42",
                    "reasoning_content": "Let me think step by step..."
                },
                "finish_reason": "stop"
            }]
        }
        """))

        assertThat(response.choices[0].message.reasoning_content, equalTo("Let me think step by step..."))
        assertThat(response.choices[0].message.reasoning, absent())
    }

    // --- Fake integration ---

    @Test
    fun `fake generates logprobs when requested (non-streaming)`() {
        val fake = FakeOpenAI()
        val openAi = OpenAI.Http(ApiKey.of("test"), fake)

        val responses = openAi(
            ChatCompletion(
                model = ModelName.of("gpt-4"),
                messages = listOf(Message.User("hello world")),
                max_tokens = MaxTokens.of(100),
                logprobs = true,
                top_logprobs = 3
            )
        ).successValue().toList()

        assertThat(responses.size, equalTo(1))
        val logprobs = responses[0].choices[0].logprobs
        assertThat(logprobs, present())
        assertThat(logprobs!!.content!!.size, greaterThan(0))

        val firstToken = logprobs.content!![0]
        assertThat(firstToken.token.isNotEmpty(), equalTo(true))
        assertThat(firstToken.top_logprobs!!.size, equalTo(3))
    }

    @Test
    fun `fake generates logprobs when requested (streaming)`() {
        val fake = FakeOpenAI()
        val openAi = OpenAI.Http(ApiKey.of("test"), fake)

        val responses = openAi(
            ChatCompletion(
                model = ModelName.of("gpt-4"),
                messages = listOf(Message.User("hello world")),
                max_tokens = MaxTokens.of(100),
                stream = true,
                logprobs = true,
                top_logprobs = 2
            )
        ).successValue().toList()

        assertThat(responses.size, greaterThan(0))
        responses.forEach { chunk ->
            val logprobs = chunk.choices[0].logprobs
            assertThat(logprobs, present())
            logprobs!!.content!!.forEach { tokenLogProb ->
                assertThat(tokenLogProb.top_logprobs!!.size, equalTo(2))
            }
        }
    }

    @Test
    fun `fake does not generate logprobs when not requested`() {
        val fake = FakeOpenAI()
        val openAi = OpenAI.Http(ApiKey.of("test"), fake)

        val responses = openAi.chatCompletion(
            ModelName.of("gpt-4"),
            listOf(Message.User("test")),
            MaxTokens.of(100),
            stream = false
        ).successValue().toList()

        assertThat(responses[0].choices[0].logprobs, absent())
    }

    // --- vLLM Filter ---

    @Test
    fun `ChatTemplateKwargs filter injects kwargs into request`() {
        var captured: String? = null
        val handler = VllmFilters.ChatTemplateKwargs(mapOf("enable_thinking" to false))
            .then { request -> captured = request.bodyString(); Response(OK) }

        handler(ChatCompletion(
            model = ModelName.of("qwen3"),
            messages = listOf(Message.User("hello")),
            max_tokens = MaxTokens.of(100)
        ).toRequest())

        val parsed = OpenAIMoshi.parse(captured!!) as MoshiObject
        val kwargs = parsed["chat_template_kwargs"] as MoshiObject
        assertThat(kwargs["enable_thinking"], equalTo(MoshiBoolean(false) as Any))
    }
}
