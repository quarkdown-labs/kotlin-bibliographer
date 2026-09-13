package com.quarkdown.bibliographer.demo

import com.quarkdown.bibliographer.Bibliographer
import com.quarkdown.bibliographer.BibliographyFormat
import com.quarkdown.bibliographer.BibliographySource
import com.quarkdown.bibliographer.StyleCatalog
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLTextAreaElement

/**
 * Source formats supported by the library's wasm backend
 * (the JVM backend additionally supports YAML, EndNote and RIS).
 */
private val SUPPORTED_FORMATS = listOf(BibliographyFormat.BIBTEX, BibliographyFormat.CSL_JSON)

private val DEFAULT_FORMAT = BibliographyFormat.BIBTEX
private const val DEFAULT_STYLE = "ieee"

fun main() {
    val format = select("format")
    val source = document.getElementById("source") as HTMLTextAreaElement

    SUPPORTED_FORMATS.forEach {
        format.addOption(value = it.name, label = "${it.name} (.${it.extension})", selected = it == DEFAULT_FORMAT)
    }
    StyleCatalog.names.sorted().forEach {
        select("style").addOption(value = it, selected = it == DEFAULT_STYLE)
    }

    source.value = SAMPLES.getValue(DEFAULT_FORMAT)
    format.addEventListener("change") {
        source.value = SAMPLES.getValue(BibliographyFormat.valueOf(format.value))
    }

    element("form").addEventListener("submit") { event ->
        event.preventDefault()
        render()
    }
    render()
}

/**
 * Reads the form, runs the bibliographer over it, and shows the result,
 * or the error that prevented one.
 */
private fun render() {
    val error = element("error")
    val output = element("output")
    try {
        val bibliographer =
            Bibliographer(
                style = select("style").value,
                source =
                    BibliographySource(
                        content = (document.getElementById("source") as HTMLTextAreaElement).value,
                        format = BibliographyFormat.valueOf(select("format").value),
                    ),
                locale = input("locale").value.trim().ifEmpty { null },
            )
        show(bibliographer)
        error.hidden = true
        output.hidden = false
    } catch (e: IllegalArgumentException) {
        output.hidden = true
        error.hidden = false
        error.textContent = e.message
    }
}

private fun show(bibliographer: Bibliographer) {
    val citation = element("citation")
    citation.textContent = ""
    val citationKeys = input("keys").value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    when (val tokens = bibliographer.citation(citationKeys)) {
        null -> citation.textContent = "(no matching citation keys)"
        else -> tokens.forEach { citation.appendChild(it.toNode()) }
    }

    val bibliography = element("bibliography")
    bibliography.textContent = ""
    bibliographer.bibliography().forEach { entry ->
        val paragraph = document.createElement("p")
        entry.label?.let { paragraph.appendChild(document.createTextNode("$it ")) }
        entry.content.forEach { paragraph.appendChild(it.toNode()) }
        bibliography.appendChild(paragraph)
    }

    element("available-keys").textContent = bibliographer.citationKeys.joinToString(", ")
}

private fun element(id: String): HTMLElement = document.getElementById(id) as HTMLElement

private fun select(id: String): HTMLSelectElement = document.getElementById(id) as HTMLSelectElement

private fun input(id: String): HTMLInputElement = document.getElementById(id) as HTMLInputElement

private fun HTMLSelectElement.addOption(
    value: String,
    label: String = value,
    selected: Boolean = false,
) {
    val option = document.createElement("option") as HTMLOptionElement
    option.value = value
    option.textContent = label
    option.selected = selected
    appendChild(option)
}
