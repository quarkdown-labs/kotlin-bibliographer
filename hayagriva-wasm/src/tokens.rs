//! Maps hayagriva's rendered [`ElemChildren`] tree onto the flat token JSON
//! the Kotlin side decodes into `BibliographyToken`s.
//!
//! The wire format is deliberately minimal — a flat array of tokens, where
//! formatting is expressed by decorator nesting, mirroring
//! `BibliographyToken.Formatted`:
//!
//! ```json
//! [{"kind": "text",   "text": "Einstein, A. "},
//!  {"kind": "italic", "child": {"kind": "text", "text": "Annalen der Physik"}},
//!  {"kind": "link",   "url": "https://doi.org/…", "label": {"kind": "text", "text": "…"}}]
//! ```

use hayagriva::citationberg::{
    FontStyle, FontVariant, FontWeight, TextDecoration, VerticalAlign,
};
use hayagriva::{ElemChild, ElemChildren, Formatting};
use serde_json::{json, Value};

/// Flattens a rendered element tree into a token array.
pub fn to_tokens(children: &ElemChildren) -> Vec<Value> {
    let mut out = Vec::new();
    push_children(children, &mut out);
    out
}

/// Renders a single element child — such as a bibliography item's
/// second-field-align label — as plain, unformatted text.
pub fn child_to_plain_text(child: &ElemChild) -> String {
    let mut out = String::new();
    push_plain_text(child, &mut out);
    out.trim().to_string()
}

fn push_children(children: &ElemChildren, out: &mut Vec<Value>) {
    for child in &children.0 {
        push_child(child, out);
    }
}

fn push_child(child: &ElemChild, out: &mut Vec<Value>) {
    match child {
        ElemChild::Text(formatted) => {
            if formatted.text.is_empty() {
                return;
            }
            out.push(decorate(text(&formatted.text), &formatted.formatting));
        }
        // Elements only carry layout metadata (CSL `display`, and which
        // construct produced them). Block/inline display is handled one level
        // up, where a bibliography item's first field is split off, so nested
        // elements simply flatten into the surrounding token run.
        ElemChild::Elem(elem) => push_children(&elem.children, out),
        ElemChild::Link { text: label, url } => out.push(json!({
            "kind": "link",
            "url": url,
            "label": decorate(text(&label.text), &label.formatting),
        })),
        // Math chunks, which hayagriva hands to the consumer as raw markup.
        // There is no math token on the Kotlin side, so the source is kept
        // verbatim as text rather than dropped.
        ElemChild::Markup(markup) => {
            if !markup.is_empty() {
                out.push(text(markup));
            }
        }
        // A placeholder for a citation rendered inside a bibliography, only
        // emitted by `hayagriva::standalone_citation`, which this crate never
        // calls. It carries no text of its own, so there is nothing to emit.
        ElemChild::Transparent { .. } => {}
    }
}

fn push_plain_text(child: &ElemChild, out: &mut String) {
    match child {
        ElemChild::Text(formatted) => out.push_str(&formatted.text),
        ElemChild::Elem(elem) => {
            for child in &elem.children.0 {
                push_plain_text(child, out);
            }
        }
        ElemChild::Link { text, .. } => out.push_str(&text.text),
        ElemChild::Markup(markup) => out.push_str(markup),
        ElemChild::Transparent { .. } => {}
    }
}

fn text(content: &str) -> Value {
    json!({"kind": "text", "text": content})
}

/// Wraps `token` in one decorator per active formatting attribute.
///
/// The attributes are independent, so the nesting order is arbitrary but
/// fixed: the innermost decorator is always the first one listed here. The
/// Kotlin side rebuilds the same nesting and flattens it when converting to
/// its own output formats.
///
/// The token schema also has an `oblique` kind, which this backend never
/// emits: citationberg's `FontStyle` models only `normal` and `italic`, so
/// there is no oblique formatting to map. The kind stays in the schema for the
/// JVM backend, whose CSL processor does distinguish it.
fn decorate(token: Value, formatting: &Formatting) -> Value {
    /// Wraps `token` in `kind` when `active`, leaving it untouched otherwise.
    fn wrap(token: Value, active: bool, kind: &str) -> Value {
        if active {
            json!({"kind": kind, "child": token})
        } else {
            token
        }
    }

    let token = wrap(token, formatting.font_style == FontStyle::Italic, "italic");
    let token = wrap(token, formatting.font_weight == FontWeight::Bold, "bold");
    let token = wrap(token, formatting.font_weight == FontWeight::Light, "light");
    let token = wrap(
        token,
        formatting.font_variant == FontVariant::SmallCaps,
        "small-caps",
    );
    let token = wrap(
        token,
        formatting.text_decoration == TextDecoration::Underline,
        "underline",
    );
    let token = wrap(token, formatting.vertical_align == VerticalAlign::Sup, "sup");
    wrap(token, formatting.vertical_align == VerticalAlign::Sub, "sub")
}

#[cfg(test)]
mod tests {
    use super::*;
    use hayagriva::{Elem, Formatted};

    fn formatted(content: &str, formatting: Formatting) -> ElemChild {
        ElemChild::Text(Formatted { text: content.to_string(), formatting })
    }

    fn children(children: Vec<ElemChild>) -> ElemChildren {
        ElemChildren(children)
    }

    #[test]
    fn plain_text_maps_to_a_single_text_token() {
        let tokens = to_tokens(&children(vec![formatted("Hello", Formatting::default())]));
        assert_eq!(tokens, vec![json!({"kind": "text", "text": "Hello"})]);
    }

    #[test]
    fn empty_text_is_dropped() {
        assert!(to_tokens(&children(vec![formatted("", Formatting::default())])).is_empty());
    }

    #[test]
    fn single_attributes_map_to_their_decorator() {
        let cases = [
            (
                Formatting { font_style: FontStyle::Italic, ..Default::default() },
                "italic",
            ),
            (
                Formatting { font_weight: FontWeight::Bold, ..Default::default() },
                "bold",
            ),
            (
                Formatting { font_weight: FontWeight::Light, ..Default::default() },
                "light",
            ),
            (
                Formatting { font_variant: FontVariant::SmallCaps, ..Default::default() },
                "small-caps",
            ),
            (
                Formatting {
                    text_decoration: TextDecoration::Underline,
                    ..Default::default()
                },
                "underline",
            ),
            (
                Formatting { vertical_align: VerticalAlign::Sup, ..Default::default() },
                "sup",
            ),
            (
                Formatting { vertical_align: VerticalAlign::Sub, ..Default::default() },
                "sub",
            ),
        ];
        for (formatting, kind) in cases {
            let tokens = to_tokens(&children(vec![formatted("x", formatting)]));
            assert_eq!(
                tokens,
                vec![json!({"kind": kind, "child": {"kind": "text", "text": "x"}})],
                "unexpected token for {kind}"
            );
        }
    }

    #[test]
    fn combined_attributes_nest_innermost_first() {
        let formatting = Formatting {
            font_style: FontStyle::Italic,
            font_weight: FontWeight::Bold,
            vertical_align: VerticalAlign::Sup,
            ..Default::default()
        };
        let tokens = to_tokens(&children(vec![formatted("x", formatting)]));
        assert_eq!(
            tokens,
            vec![json!({
                "kind": "sup",
                "child": {
                    "kind": "bold",
                    "child": {"kind": "italic", "child": {"kind": "text", "text": "x"}},
                },
            })]
        );
    }

    #[test]
    fn nested_elements_are_flattened_in_order() {
        let inner = Elem {
            children: children(vec![
                formatted("b", Formatting::default()),
                formatted("c", Formatting::default()),
            ]),
            display: None,
            meta: None,
        };
        let tokens = to_tokens(&children(vec![
            formatted("a", Formatting::default()),
            ElemChild::Elem(inner),
        ]));
        assert_eq!(
            tokens.iter().map(|t| t["text"].as_str().unwrap()).collect::<Vec<_>>(),
            vec!["a", "b", "c"]
        );
    }

    #[test]
    fn links_carry_their_url_and_a_decorated_label() {
        let tokens = to_tokens(&children(vec![ElemChild::Link {
            text: Formatted {
                text: "10.1000/x".to_string(),
                formatting: Formatting {
                    font_style: FontStyle::Italic,
                    ..Default::default()
                },
            },
            url: "https://doi.org/10.1000/x".to_string(),
        }]));
        assert_eq!(
            tokens,
            vec![json!({
                "kind": "link",
                "url": "https://doi.org/10.1000/x",
                "label": {"kind": "italic", "child": {"kind": "text", "text": "10.1000/x"}},
            })]
        );
    }

    #[test]
    fn markup_is_kept_as_text() {
        let tokens = to_tokens(&children(vec![ElemChild::Markup("E = mc^2".to_string())]));
        assert_eq!(tokens, vec![json!({"kind": "text", "text": "E = mc^2"})]);
    }

    #[test]
    fn plain_text_ignores_formatting_and_trims() {
        let child = ElemChild::Elem(Elem {
            children: children(vec![
                formatted(
                    " [1",
                    Formatting { font_weight: FontWeight::Bold, ..Default::default() },
                ),
                formatted("] ", Formatting::default()),
            ]),
            display: None,
            meta: None,
        });
        assert_eq!(child_to_plain_text(&child), "[1]");
    }
}
