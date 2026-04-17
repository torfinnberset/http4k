package org.http4k.connect.openai

import org.http4k.core.Filter
import org.http4k.core.Method.POST
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiObject
import org.http4k.format.wrap

/**
 * Filters for vLLM-specific extensions to the OpenAI-compatible API.
 *
 * Usage:
 * ```
 * val http = VllmFilters.ChatTemplateKwargs(mapOf("enable_thinking" to false))
 *     .then(JavaHttpClient())
 *
 * val vllm = OpenAI.Http(ApiKey.of("token"), http)
 * ```
 */
object VllmFilters {

    /**
     * Injects chat_template_kwargs into chat completion request bodies.
     * Used for vLLM-specific features like Qwen3's enable_thinking parameter.
     *
     * This field is not part of the OpenAI spec.
     */
    fun ChatTemplateKwargs(kwargs: Map<String, Any>) = Filter { next ->
        { request ->
            if (request.method == POST && request.uri.path.endsWith("/chat/completions")) {
                val body = OpenAIMoshi.parse(request.bodyString()) as MoshiObject
                body.attributes["chat_template_kwargs"] = MoshiNode.wrap(kwargs)
                next(request.body(OpenAIMoshi.asFormatString(body)))
            } else {
                next(request)
            }
        }
    }
}
