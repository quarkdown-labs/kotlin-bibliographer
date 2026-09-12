//! The wrapper's platform-independent core: parsing a bibliography source and
//! a CSL style, then rendering citations and bibliographies as the token JSON
//! defined in [`crate::tokens`]. The wasm-bindgen exports are a thin shell
//! around [`Bibliographer`], so everything here is testable with plain
//! `cargo test`.

use hayagriva::archive::ArchivedStyle;
use hayagriva::citationberg::json::Item as CslJsonItem;
use hayagriva::citationberg::{IndependentStyle, Locale, LocaleCode, Style};
use hayagriva::io::from_biblatex_str;
use hayagriva::{
    BibliographyDriver, BibliographyRequest, CitationItem, CitationRequest, Entry, Rendered,
};
use std::sync::OnceLock;

use crate::tokens;

/// The CSL locale definitions a style falls back to for its localized terms,
/// embedded through hayagriva's `archive` feature and parsed once on first use.
fn locales() -> &'static [Locale] {
    static LOCALES: OnceLock<Vec<Locale>> = OnceLock::new();
    LOCALES.get_or_init(hayagriva::archive::locales)
}

/// Resolves a style argument: CSL XML content is parsed directly, anything
/// else is looked up in hayagriva's embedded style archive.
///
/// The Zotero URL form is tried first: hayagriva's friendly aliases
/// (`"chicago-notes"`, `"springer-basic"`, ...) can point at a different
/// style file than the identically-named Zotero id, and matching Zotero ids
/// exactly keeps by-name resolution consistent with the JVM backend.
fn resolve_style(style: &str) -> Result<IndependentStyle, String> {
    if style.trim_start().starts_with('<') {
        return IndependentStyle::from_xml(style).map_err(|e| format!("Invalid CSL style: {e}"));
    }
    let archived = ArchivedStyle::by_id(&format!("http://www.zotero.org/styles/{style}"))
        .or_else(|| ArchivedStyle::by_name(style))
        .ok_or_else(|| format!("Unknown style name: {style}"))?;
    match archived.get() {
        Style::Independent(style) => Ok(style),
        Style::Dependent(_) => Err(format!("Style is not independently usable: {style}")),
    }
}

/// Which bibliography source format is being parsed.
pub enum SourceFormat {
    Bibtex,
    CslJson,
}

impl SourceFormat {
    /// Parse a format identifier as used on the Kotlin/JS side of the
    /// wrapper. Accepts exactly `"bibtex"` and `"csl-json"`.
    pub fn parse(s: &str) -> Option<Self> {
        match s {
            "bibtex" => Some(Self::Bibtex),
            "csl-json" => Some(Self::CslJson),
            _ => None,
        }
    }
}

/// The parsed bibliography, in whichever native representation its source
/// format produces.
///
/// hayagriva 0.10.1 cannot convert `citationberg::json::Item` (csl-json's
/// item type) into its own `Entry`; both implement its crate-private
/// `EntryLike` trait and are accepted directly by the generic rendering
/// pipeline. Each source format therefore keeps its native item type, and
/// [`Bibliographer::render`] dispatches on this enum with one concrete
/// driver set-up per branch.
enum Entries {
    Bibliography(Vec<Entry>),
    CslJson(Vec<CslJsonItem>),
}

pub struct Bibliographer {
    style: IndependentStyle,
    entries: Entries,
    /// The citation key of every entry, in source order: index `i` matches
    /// index `i` in [`Entries`], which [`Self::resolve`] relies on.
    /// Computed once at construction.
    keys: Vec<String>,
    locale: Option<LocaleCode>,
}

impl Bibliographer {
    pub fn new(
        style: &str,
        source: &str,
        format: SourceFormat,
        locale: Option<&str>,
    ) -> Result<Self, String> {
        let style = resolve_style(style)?;
        let entries = match format {
            SourceFormat::Bibtex => {
                let parsed: Vec<Entry> = from_biblatex_str(source)
                    .map_err(|errors| {
                        let details: Vec<String> =
                            errors.iter().map(ToString::to_string).collect();
                        format!("Invalid BibTeX source: {}", details.join("; "))
                    })?
                    .into_iter()
                    .collect();
                // biblatex silently skips anything that is not an `@`-prefixed
                // entry, parsing garbage to zero entries, so an empty result
                // is treated as invalid input.
                if parsed.is_empty() {
                    return Err("Invalid BibTeX source: no entries found".to_string());
                }
                Entries::Bibliography(parsed)
            }
            SourceFormat::CslJson => {
                let parsed: Vec<CslJsonItem> = serde_json::from_str(source)
                    .map_err(|e| format!("Invalid CSL-JSON source: {e}"))?;
                if parsed.is_empty() {
                    return Err("Invalid CSL-JSON source: no entries found".to_string());
                }
                // CSL-JSON requires a unique `id` on every item, and hayagriva
                // uses it as the citation key: an item without one can neither
                // be cited nor matched to its bibliography entry, and a
                // repeated one is unciteable past its first occurrence.
                let mut seen = std::collections::HashSet::new();
                for (position, item) in parsed.iter().enumerate() {
                    let Some(id) = item.id() else {
                        return Err(format!(
                            "Invalid CSL-JSON source: item at index {position} has no \"id\""
                        ));
                    };
                    if !seen.insert(id.to_string()) {
                        return Err(format!(
                            "Invalid CSL-JSON source: duplicate item id \"{id}\""
                        ));
                    }
                }
                Entries::CslJson(parsed)
            }
        };
        let keys = match &entries {
            Entries::Bibliography(entries) => {
                entries.iter().map(|e| e.key().to_string()).collect()
            }
            // Items without an "id" were rejected above, so nothing is
            // filtered out here and the result stays index-aligned.
            Entries::CslJson(items) => {
                items.iter().filter_map(|i| i.id().map(|id| id.to_string())).collect()
            }
        };
        let locale = locale.map(|s| LocaleCode(s.to_string()));
        Ok(Self { style, entries, keys, locale })
    }

    /// The citation key of every entry, in source order.
    pub fn citation_keys(&self) -> &[String] {
        &self.keys
    }

    /// Renders the combined in-text citation for `keys`, as a JSON token
    /// array. Keys that name no entry are ignored; if none of them match,
    /// the render is empty and the empty string is returned.
    pub fn citation(&self, keys: &[String]) -> String {
        let cited = self.resolve(keys);
        if cited.is_empty() {
            return String::new();
        }
        let rendered = self.render(Some(&cited));
        // The visible request is appended last, after the hidden registration
        // one; see `render`.
        let Some(citation) = rendered.citations.last() else {
            return String::new();
        };
        let tokens = tokens::to_tokens(&citation.citation);
        if tokens.is_empty() {
            String::new()
        } else {
            serde_json::to_string(&tokens).expect("token JSON is always serializable")
        }
    }

    /// Renders every entry as a JSON array of
    /// `{"citationKey", "label", "content"}` objects, in the order the style's
    /// sorting rules dictate.
    pub fn bibliography(&self) -> String {
        let rendered = self.render(None);
        let items: Vec<serde_json::Value> = rendered
            .bibliography
            .iter()
            .flat_map(|bibliography| &bibliography.items)
            .map(|item| {
                serde_json::json!({
                    "citationKey": item.key,
                    // `first_field` is populated exactly for styles that ask
                    // for `second-field-align`, the numeric label of e.g.
                    // IEEE, which is presented separately from the content.
                    "label": item.first_field.as_ref().map(tokens::child_to_plain_text),
                    "content": tokens::to_tokens(&item.content),
                })
            })
            .collect();
        serde_json::to_string(&items).expect("bibliography JSON is always serializable")
    }

    /// Maps citation keys onto entry indices, dropping unknown ones.
    fn resolve(&self, keys: &[String]) -> Vec<usize> {
        keys.iter()
            .filter_map(|key| self.keys.iter().position(|candidate| candidate == key))
            .collect()
    }

    /// Runs the CSL machinery over the whole bibliography, optionally adding a
    /// visible citation for the entries at the given indices.
    ///
    /// hayagriva's `EntryLike` trait (which the driver types are generic
    /// over) is not exported, so the shared driver set-up is expanded once
    /// per [`Entries`] variant through this macro, each expansion
    /// type-checked against its own item type. [`Rendered`] is not generic,
    /// so everything downstream is ordinary shared code.
    ///
    /// The leading citation is hidden: it registers every entry without
    /// contributing output. hayagriva numbers entries in first-encounter
    /// order, so registering them all makes numbering a property of the
    /// whole bibliography, matching the JVM backend and the entry labels.
    ///
    /// Hidden items mark their entries as seen, so `position="first"` tests
    /// false for every citation; this API renders citations in isolation,
    /// where first-versus-subsequent is unknowable anyway.
    fn render(&self, cited: Option<&[usize]>) -> Rendered {
        macro_rules! render_over {
            ($entries:expr) => {{
                let entries = $entries;
                let request = |items| {
                    CitationRequest::new(items, &self.style, self.locale.clone(), locales(), None)
                };
                let mut driver = BibliographyDriver::new();
                let registration = entries
                    .iter()
                    .map(|entry| {
                        let mut item = CitationItem::with_entry(entry);
                        item.hidden = true;
                        item
                    })
                    .collect();
                driver.citation(request(registration));
                if let Some(cited) = cited {
                    let items =
                        cited.iter().map(|&i| CitationItem::with_entry(&entries[i])).collect();
                    driver.citation(request(items));
                }
                driver.finish(self.bibliography_request())
            }};
        }

        match &self.entries {
            Entries::Bibliography(entries) => render_over!(entries),
            Entries::CslJson(entries) => render_over!(entries),
        }
    }

    /// The bibliography-wide half of a render request, shared by both branches
    /// of [`Self::render`] because it does not mention the entry type.
    fn bibliography_request(&self) -> BibliographyRequest<'_> {
        BibliographyRequest {
            style: &self.style,
            locale: self.locale.clone(),
            locale_files: locales(),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const BIBTEX: &str = r#"
@article{einstein1905,
    author = {Einstein, Albert},
    title = {Zur Elektrodynamik bewegter K{\"o}rper},
    journal = {Annalen der Physik},
    year = {1905}
}
@book{hawking1988,
    author = {Hawking, Stephen},
    title = {A Brief History of Time},
    year = {1988}
}
"#;

    const CSL_JSON: &str = r#"[
        {"id": "einstein1905", "type": "article-journal",
         "title": "Zur Elektrodynamik bewegter Körper",
         "author": [{"family": "Einstein", "given": "Albert"}],
         "issued": {"date-parts": [[1905]]},
         "container-title": "Annalen der Physik"},
        {"id": "hawking1988", "type": "book", "title": "A Brief History of Time",
         "author": [{"family": "Hawking", "given": "Stephen"}],
         "issued": {"date-parts": [[1988]]}}
    ]"#;

    fn ieee() -> String { std::fs::read_to_string("tests/styles/ieee.csl").unwrap() }

    fn apa() -> String { std::fs::read_to_string("tests/styles/apa.csl").unwrap() }

    /// Flattens a token array back to its plain text, mirroring the Kotlin
    /// side's `toPlainText` so the fidelity assertions read like the JVM
    /// backend's.
    fn plain(tokens: &serde_json::Value) -> String {
        fn one(t: &serde_json::Value) -> String {
            match t["kind"].as_str().unwrap() {
                "text" => t["text"].as_str().unwrap().to_string(),
                "link" => one(&t["label"]),
                _ => one(&t["child"]),
            }
        }
        tokens.as_array().unwrap().iter().map(one).collect()
    }

    fn citation_tokens(b: &Bibliographer, keys: &[&str]) -> serde_json::Value {
        let keys: Vec<String> = keys.iter().map(|k| k.to_string()).collect();
        serde_json::from_str(&b.citation(&keys)).unwrap()
    }

    #[test]
    fn bibtex_keys_in_source_order() {
        let b = Bibliographer::new(&ieee(), BIBTEX, SourceFormat::Bibtex, None).unwrap();
        assert_eq!(b.citation_keys(), vec!["einstein1905", "hawking1988"]);
    }

    #[test]
    fn csl_json_keys_in_source_order() {
        let b = Bibliographer::new(&ieee(), CSL_JSON, SourceFormat::CslJson, None).unwrap();
        assert_eq!(b.citation_keys(), vec!["einstein1905", "hawking1988"]);
    }

    #[test]
    fn invalid_source_is_an_error() {
        assert!(Bibliographer::new(&ieee(), "not bibtex {{{", SourceFormat::Bibtex, None).is_err());
    }

    #[test]
    fn unknown_style_names_are_an_error() {
        assert!(Bibliographer::new("not-a-style", BIBTEX, SourceFormat::Bibtex, None).is_err());
    }

    #[test]
    fn invalid_style_xml_is_an_error() {
        assert!(Bibliographer::new("<style>truncated", BIBTEX, SourceFormat::Bibtex, None).is_err());
    }

    /// Archived styles must resolve by their Zotero id and render like the
    /// equivalent XML-supplied style, so by-name behavior matches the JVM
    /// backend's classpath resolution.
    #[test]
    fn archived_styles_resolve_by_zotero_id() {
        let by_name = Bibliographer::new("ieee", BIBTEX, SourceFormat::Bibtex, None).unwrap();
        assert_eq!(plain(&citation_tokens(&by_name, &["einstein1905"])), "[1]");
        assert_eq!(
            plain(&citation_tokens(&by_name, &["hawking1988"])),
            plain(&citation_tokens(
                &Bibliographer::new(&ieee(), BIBTEX, SourceFormat::Bibtex, None).unwrap(),
                &["hawking1988"],
            ))
        );
    }

    /// The Zotero URL form must win over hayagriva's friendly aliases:
    /// `chicago-notes-bibliography` is an exact Zotero id and must reach the
    /// style file of the same name, not whatever an alias happens to map to.
    #[test]
    fn zotero_ids_resolve_to_their_exact_style() {
        let b = Bibliographer::new(
            "chicago-notes-bibliography",
            BIBTEX,
            SourceFormat::Bibtex,
            None,
        );
        assert!(b.is_ok());
    }

    #[test]
    fn csl_json_items_without_an_id_are_rejected() {
        let source = r#"[{"type": "book", "title": "Anonymous"}]"#;
        assert!(Bibliographer::new(&ieee(), source, SourceFormat::CslJson, None).is_err());
    }

    #[test]
    fn csl_json_duplicate_ids_are_rejected() {
        let source = r#"[
            {"id": "dup", "type": "book", "title": "First"},
            {"id": "dup", "type": "book", "title": "Second"}
        ]"#;
        assert!(Bibliographer::new(&ieee(), source, SourceFormat::CslJson, None).is_err());
    }

    #[test]
    fn ieee_citations() {
        let b = Bibliographer::new(&ieee(), BIBTEX, SourceFormat::Bibtex, None).unwrap();
        assert_eq!(plain(&citation_tokens(&b, &["einstein1905"])), "[1]");
        assert_eq!(
            plain(&citation_tokens(&b, &["einstein1905", "hawking1988"])),
            "[1], [2]"
        );
    }

    /// Citation numbers are a property of the whole bibliography, not of the
    /// individual `citation` call: citing the second entry alone must still
    /// render `[2]`, matching both its bibliography label and the JVM
    /// (citeproc-java) backend, which pre-registers every entry.
    #[test]
    fn ieee_citation_numbers_are_global() {
        let b = Bibliographer::new(&ieee(), BIBTEX, SourceFormat::Bibtex, None).unwrap();
        assert_eq!(plain(&citation_tokens(&b, &["hawking1988"])), "[2]");
    }

    #[test]
    fn ieee_bibliography_has_labels_and_sorted_keys() {
        let b = Bibliographer::new(&ieee(), BIBTEX, SourceFormat::Bibtex, None).unwrap();
        let bib: serde_json::Value = serde_json::from_str(&b.bibliography()).unwrap();
        let items = bib.as_array().unwrap();
        assert_eq!(items[0]["citationKey"], "einstein1905");
        assert_eq!(items[0]["label"], "[1]");
        assert!(plain(&items[0]["content"]).contains("Einstein"));
        assert_eq!(items[1]["label"], "[2]");
    }

    #[test]
    fn apa_citation_and_unlabeled_sorted_bibliography() {
        let b = Bibliographer::new(&apa(), BIBTEX, SourceFormat::Bibtex, None).unwrap();
        assert_eq!(plain(&citation_tokens(&b, &["einstein1905"])), "(Einstein, 1905)");
        let bib: serde_json::Value = serde_json::from_str(&b.bibliography()).unwrap();
        for item in bib.as_array().unwrap() {
            assert!(item["label"].is_null());
        }
    }

    #[test]
    fn apa_italicizes_container_title() {
        let b = Bibliographer::new(&apa(), BIBTEX, SourceFormat::Bibtex, None).unwrap();
        let bib: serde_json::Value = serde_json::from_str(&b.bibliography()).unwrap();
        let einstein = &bib.as_array().unwrap()[0];
        let json = einstein["content"].to_string();
        assert!(json.contains("\"kind\":\"italic\""), "expected italic journal name: {json}");
    }

    #[test]
    fn csl_json_sources_render_like_bibtex_ones() {
        let b = Bibliographer::new(&ieee(), CSL_JSON, SourceFormat::CslJson, None).unwrap();
        assert_eq!(plain(&citation_tokens(&b, &["hawking1988"])), "[2]");
        let bib: serde_json::Value = serde_json::from_str(&b.bibliography()).unwrap();
        let items = bib.as_array().unwrap();
        assert_eq!(items[0]["citationKey"], "einstein1905");
        assert_eq!(items[0]["label"], "[1]");
        assert!(plain(&items[0]["content"]).contains("Einstein"));
    }

    #[test]
    fn empty_render_returns_empty_string() {
        let b = Bibliographer::new(&ieee(), BIBTEX, SourceFormat::Bibtex, None).unwrap();
        assert_eq!(b.citation(&[]), "");
    }

    #[test]
    fn unknown_keys_are_ignored_among_known_ones() {
        let b = Bibliographer::new(&ieee(), BIBTEX, SourceFormat::Bibtex, None).unwrap();
        assert_eq!(b.citation(&["unknown".to_string()]), "");
        assert_eq!(plain(&citation_tokens(&b, &["einstein1905", "unknown"])), "[1]");
    }

    /// Locale terms come from the embedded `locales-*.xml` files, so this also
    /// proves the build-time locale bundle is reachable and correctly indexed
    /// by locale code: IEEE renders the page label as `pp.` in English and as
    /// `S.` in German.
    #[test]
    fn locale_override_selects_localized_terms() {
        const PAGED: &str = r#"
@article{einstein1905,
    author = {Einstein, Albert},
    title = {Zur Elektrodynamik bewegter K{\"o}rper},
    journal = {Annalen der Physik},
    pages = {891--921},
    year = {1905}
}
"#;
        let render = |locale| {
            let b = Bibliographer::new(&ieee(), PAGED, SourceFormat::Bibtex, locale).unwrap();
            b.bibliography()
        };
        assert!(render(Some("en-US")).contains("pp."), "{}", render(Some("en-US")));
        assert!(render(Some("de-DE")).contains("S."), "{}", render(Some("de-DE")));
    }
}
