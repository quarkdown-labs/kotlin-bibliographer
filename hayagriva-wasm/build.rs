//! Generates the list of CSL locale files to embed in the binary.
//!
//! hayagriva is built without its `archive` feature (which would bundle a
//! whole style/locale archive), so the locale definitions a style falls back
//! to for its terms have to be supplied by this crate. `locales/` holds the
//! CSL project's `locales-*.xml` files, fetched by `regenerate.sh` and left
//! out of version control; this script turns whatever is there into an
//! `include_str!` array so the list never needs manual maintenance.

use std::{env, fs, path::Path};

/// Where the fetched locale XMLs live, relative to the crate root.
const LOCALES_DIR: &str = "locales";

/// Told to the developer whenever the locale bundle is missing: CI runs the
/// fetch as part of `regenerate.sh`, but a fresh local checkout has not.
const MISSING_LOCALES_HINT: &str = "\
The CSL locale definitions are gitignored and must be fetched before building:
run `hayagriva-wasm/regenerate.sh` (which fetches them), or clone
https://github.com/citation-style-language/locales into hayagriva-wasm/locales.";

fn main() {
    println!("cargo:rerun-if-changed={LOCALES_DIR}");

    let mut files: Vec<_> = fs::read_dir(LOCALES_DIR)
        .unwrap_or_else(|e| {
            panic!("hayagriva-wasm/{LOCALES_DIR}/ cannot be read: {e}\n{MISSING_LOCALES_HINT}")
        })
        .map(|entry| entry.expect("cannot read locale directory entry").path())
        .filter(|path| path.extension().is_some_and(|ext| ext == "xml"))
        .collect();
    // Deterministic order keeps the generated file (and thus the build cache)
    // stable across filesystems that enumerate directories differently.
    files.sort();

    assert!(
        !files.is_empty(),
        "hayagriva-wasm/{LOCALES_DIR}/ contains no *.xml files.\n{MISSING_LOCALES_HINT}"
    );

    let mut generated = String::from("static LOCALE_XML: &[&str] = &[\n");
    for file in &files {
        let name = file
            .file_name()
            .and_then(|n| n.to_str())
            .expect("locale filename is not valid UTF-8");
        // Paths are resolved against CARGO_MANIFEST_DIR because the generated
        // file is `include!`d from OUT_DIR, where relative paths would not
        // point back at the crate.
        generated.push_str(&format!(
            "    include_str!(concat!(env!(\"CARGO_MANIFEST_DIR\"), \"/{LOCALES_DIR}/{name}\")),\n"
        ));
        println!("cargo:rerun-if-changed={LOCALES_DIR}/{name}");
    }
    generated.push_str("];\n");

    let out = Path::new(&env::var("OUT_DIR").expect("OUT_DIR is set by cargo"))
        .join("locale_includes.rs");
    fs::write(&out, generated).unwrap_or_else(|e| panic!("cannot write {}: {e}", out.display()));
}
