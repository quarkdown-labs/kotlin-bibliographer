#!/usr/bin/env bash
# Rebuilds the wasm binary and re-vendors the npm-shaped `hayagriva-wasm`
# package consumed by the Kotlin/JS (wasmJs) target.
#
# Non-interactive; must run identically on macOS (dev) and Linux (CI). CI
# must run this script before `cargo test`, because `build.rs` requires the
# locale bundle this script fetches (see hayagriva-wasm/build.rs).
#
# Output is meant to be byte-identical across runs on the same crate
# version/toolchain: a CI drift check diffs the vendored package directory
# against what is committed, so nothing here should embed a timestamp,
# absolute path, or other non-reproducible value.
set -euo pipefail
cd "$(dirname "$0")"

# `--check` additionally verifies (after regenerating) that the vendored
# package matches what is committed — the CI drift check. Keeping the check
# here, next to PKG_DIR, means the workflow never hardcodes the package path.
CHECK=false
if [ "${1:-}" = "--check" ]; then
    CHECK=true
fi

# Pinned to a specific commit of citation-style-language/locales (rather than
# a branch) so the locale bundle — and thus build output — does not drift
# out from under this script when upstream publishes new locales.
LOCALES_COMMIT="2866970c249ab8bae513b41ee6241ccdcdf967eb"

# Pinned so the wasm-pack build (step 3) is reproducible across machines; verified
# working on this machine. Bump deliberately, not implicitly via a stale cargo cache.
WASM_PACK_VERSION="0.15.0"

# The vendoring target: an npm-shaped package directory, not a resources/
# path — klib resources are not propagated to a consumer's linked output on
# Kotlin 2.4.20, whereas npm dependencies are.
PKG_DIR="../src/wasmJsMain/npm/hayagriva-wasm"

# --- 1. Toolchain -----------------------------------------------------------
# rust-toolchain.toml pins the channel; rustup reads it from cwd.
rustup target add wasm32-unknown-unknown

installed_wasm_pack_version=""
if command -v wasm-pack >/dev/null 2>&1; then
    installed_wasm_pack_version="$(wasm-pack --version | awk '{print $2}')"
fi
if [ "$installed_wasm_pack_version" != "$WASM_PACK_VERSION" ]; then
    cargo install wasm-pack --version "$WASM_PACK_VERSION" --locked
fi

# --- 2. Locales, pinned ------------------------------------------------------
# Must run before any cargo build below: build.rs fails fast if locales/ is
# absent or empty.
if [ ! -f "locales/.commit" ] || [ "$(cat locales/.commit)" != "$LOCALES_COMMIT" ]; then
    rm -rf locales && mkdir locales
    tmp=$(mktemp -d)
    git clone --quiet https://github.com/citation-style-language/locales.git "$tmp"
    git -C "$tmp" checkout --quiet "$LOCALES_COMMIT"
    cp "$tmp"/locales-*.xml locales/
    echo "$LOCALES_COMMIT" > locales/.commit
    rm -rf "$tmp"
fi

# --- 3. Build -----------------------------------------------------------
# `--no-pack` skips wasm-pack's own package.json generation: that output
# embeds the installed wasm-pack version and isn't shaped the way we need
# (no "type": "module", no loader.mjs entry point), so it would both be
# non-deterministic across machines and wrong. Step 4 writes our own
# deterministic package.json instead.
# `--release` already runs `wasm-opt` when available.
wasm-pack build --target web --release --no-pack --out-name hayagriva_wasm

# --- 4. Assemble the vendored package ---------------------------------------
# Start from a clean directory so a file removed from `pkg/` (e.g. a .d.ts
# wasm-pack stops emitting in a future version) doesn't linger as stale
# vendored cruft.
rm -rf "$PKG_DIR"
mkdir -p "$PKG_DIR"

cp pkg/hayagriva_wasm.js pkg/hayagriva_wasm_bg.wasm "$PKG_DIR/"
# loader.mjs is hand-written source (hayagriva-wasm/js/loader.mjs), never
# generated or edited by this script — only copied into place.
cp js/loader.mjs "$PKG_DIR/"

# wasm-bindgen's .d.ts outputs are optional artifacts of the target/profile
# combination in use; vendor them when present so downstream TypeScript
# consumers get types, without hard-failing when they are not produced.
dts_files=()
for f in pkg/hayagriva_wasm.d.ts pkg/hayagriva_wasm_bg.wasm.d.ts; do
    if [ -f "$f" ]; then
        cp "$f" "$PKG_DIR/"
        dts_files+=("$(basename "$f")")
    fi
done

# Crate version is the single source of truth for the package version, so
# the two can never drift apart.
version="$(sed -n 's/^version *= *"\([^"]*\)".*/\1/p' Cargo.toml | head -n1)"
if [ -z "$version" ]; then
    echo "regenerate.sh: could not read [package].version from Cargo.toml" >&2
    exit 1
fi

# `files` is listed in a fixed order (loader first, then wasm-bindgen's own
# outputs, then any .d.ts) so re-running this script never reorders it.
files_json="\"loader.mjs\", \"hayagriva_wasm.js\", \"hayagriva_wasm_bg.wasm\""
for f in "${dts_files[@]}"; do
    files_json="$files_json, \"$f\""
done

cat > "$PKG_DIR/package.json" <<EOF
{
  "name": "hayagriva-wasm",
  "version": "$version",
  "type": "module",
  "main": "./loader.mjs",
  "module": "./loader.mjs",
  "exports": {
    ".": "./loader.mjs"
  },
  "files": [$files_json]
}
EOF

echo "Vendored $PKG_DIR:"
echo "  $(du -h "$PKG_DIR/hayagriva_wasm_bg.wasm" | cut -f1)  hayagriva_wasm_bg.wasm"

if $CHECK; then
    # `git status --porcelain` (not `git diff`) so untracked files fail too.
    drift="$(git status --porcelain -- "$PKG_DIR")"
    if [ -n "$drift" ]; then
        echo "::error::Vendored hayagriva binding is out of date. Run hayagriva-wasm/regenerate.sh and commit the result."
        echo "$drift"
        exit 1
    fi
fi
