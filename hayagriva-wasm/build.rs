//! Embeds the catalog style definitions from the repository's shared
//! `styles/` directory: the same files the JVM artifact embeds, keeping the
//! catalog byte-identical across platforms.

use std::{env, fs, path::Path};

const STYLES_DIR: &str = "../styles";

fn main() {
    println!("cargo:rerun-if-changed={STYLES_DIR}");

    let mut files: Vec<_> = fs::read_dir(STYLES_DIR)
        .expect("the repository's styles/ directory is readable")
        .map(|entry| entry.expect("readable styles/ entry").path())
        .filter(|path| path.extension().is_some_and(|ext| ext == "csl"))
        .collect();
    // Deterministic order keeps the generated file stable across filesystems.
    files.sort();
    assert!(!files.is_empty(), "styles/ contains no .csl files");

    let mut generated = String::from("static CATALOG_STYLES: &[(&str, &str)] = &[\n");
    for file in &files {
        let name = file
            .file_stem()
            .and_then(|n| n.to_str())
            .expect("style filename is valid UTF-8");
        generated.push_str(&format!(
            "    (\"{name}\", include_str!(concat!(env!(\"CARGO_MANIFEST_DIR\"), \"/{STYLES_DIR}/{name}.csl\"))),\n"
        ));
    }
    generated.push_str("];\n");

    let out = Path::new(&env::var("OUT_DIR").expect("OUT_DIR is set by cargo")).join("catalog_styles.rs");
    fs::write(&out, generated).unwrap_or_else(|e| panic!("cannot write {}: {e}", out.display()));
}
