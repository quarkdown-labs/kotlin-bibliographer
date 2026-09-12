//! The wasm-bindgen boundary: a thin shell around [`core::Bibliographer`]
//! that exposes it to JavaScript as a class. All parsing/rendering logic
//! lives in [`crate::core`] and is exercised by plain `cargo test`; this
//! module only translates between JS-friendly types (strings, JSON) and the
//! core's own.

use wasm_bindgen::prelude::*;

use crate::core;

#[wasm_bindgen]
pub struct Bibliographer(core::Bibliographer);

#[wasm_bindgen]
impl Bibliographer {
    #[wasm_bindgen(constructor)]
    pub fn new(
        style: &str,
        source: &str,
        format: &str,
        locale: Option<String>,
    ) -> Result<Bibliographer, JsError> {
        let format = core::SourceFormat::parse(format)
            .ok_or_else(|| JsError::new(&format!("Unsupported source format: {format}")))?;
        core::Bibliographer::new(style, source, format, locale.as_deref())
            .map(Bibliographer)
            .map_err(|e| JsError::new(&e))
    }

    pub fn citation_keys(&self) -> String {
        serde_json::to_string(&self.0.citation_keys()).unwrap()
    }

    pub fn citation(&self, keys_json: &str) -> Result<String, JsError> {
        let keys: Vec<String> = serde_json::from_str(keys_json)
            .map_err(|e| JsError::new(&format!("Invalid keys JSON: {e}")))?;
        Ok(self.0.citation(&keys))
    }

    pub fn bibliography(&self) -> String {
        self.0.bibliography()
    }
}
