@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.quarkdown.bibliographer.hayagriva

/**
 * The vendored [hayagriva](https://github.com/typst/hayagriva) binding,
 * distributed as the npm-shaped package `hayagriva-wasm` (vendored in-repo,
 * see `src/wasmJsMain/npm/hayagriva-wasm/`) and initialized by its `loader.mjs`
 * under top-level await.
 *
 * All methods exchange JSON strings; see the wrapper crate (`hayagriva-wasm/`)
 * for the token schema.
 */
@Suppress("ktlint:standard:function-naming")
@JsModule("hayagriva-wasm")
@JsName("Bibliographer")
internal external class HayagrivaEngine(
    style: String,
    source: String,
    format: String,
    locale: String?,
) : JsAny {
    fun citation_keys(): String

    fun citation(keysJson: String): String

    fun bibliography(): String
}

// JS glue below: each `js(...)` snippet becomes the body of a JS function
// whose parameters carry the Kotlin parameter names, so the identifiers in
// the snippets must match the Kotlin signatures exactly.

internal fun jsonParse(json: String): JsAny? = js("JSON.parse(json)")

internal fun isJsArray(value: JsAny): Boolean = js("Array.isArray(value)")

internal fun arrayLength(array: JsAny): Int = js("array.length")

internal fun arrayElement(
    array: JsAny,
    index: Int,
): JsAny = js("array[index]")

internal fun stringProperty(
    obj: JsAny,
    key: String,
): String? = js("typeof obj[key] === 'string' ? obj[key] : null")

internal fun objectProperty(
    obj: JsAny,
    key: String,
): JsAny? = js("obj[key] ?? null")

internal fun elementToString(value: JsAny): String = js("String(value)")

internal fun newJsArray(): JsAny = js("[]")

internal fun pushString(
    array: JsAny,
    value: String,
) {
    js("array.push(value)")
}

internal fun jsonStringify(value: JsAny): String = js("JSON.stringify(value)")

/**
 * Parses [json] and returns the result only if it is a JS array.
 */
internal fun parseJsonArrayOrNull(json: String): JsAny? = jsonParse(json)?.takeIf(::isJsArray)

/**
 * Maps every element of a JS array with [transform], in index order.
 */
internal inline fun <T> JsAny.mapElements(transform: (JsAny) -> T): List<T> =
    buildList {
        for (index in 0 until arrayLength(this@mapElements)) {
            add(transform(arrayElement(this@mapElements, index)))
        }
    }

/**
 * Serializes a list of strings as a JSON array via `JSON.stringify`,
 * so quotes, backslashes and control characters round-trip correctly.
 */
internal fun stringsToJson(values: List<String>): String {
    val array = newJsArray()
    values.forEach { pushString(array, it) }
    return jsonStringify(array)
}
