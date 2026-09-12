#!/usr/bin/env bash
# Rebuilds the wasm binary and re-vendors the npm-shaped `hayagriva-wasm`
# package consumed by the Kotlin/JS (wasmJs) target.
#
# Non-interactive. Output is meant to be byte-identical across runs and
# machines, so nothing here may embed a timestamp, absolute path, or other
# non-reproducible value.
set -euo pipefail
cd "$(dirname "$0")"

# Linux is the authoritative build platform: rustc's output is deterministic
# per host platform but not across platforms (constant ordering differs
# between e.g. macOS-arm64 and Linux-x64 hosts). On any other system,
# delegate the whole run to a pinned Linux container so regeneration always
# produces the same bytes, no matter which machine runs it.
if [ "$(uname -s)" != "Linux" ]; then
    command -v docker >/dev/null 2>&1 || {
        echo "regenerate.sh: building the binding requires Linux; install Docker so the build can run in a container." >&2
        exit 1
    }
    repo_root="$(cd .. && pwd)"
    exec docker run --rm --platform linux/amd64 \
        -v "$repo_root":/work -w /work/hayagriva-wasm \
        -e CARGO_TARGET_DIR=/work/hayagriva-wasm/target/container \
        -e GIT_CONFIG_COUNT=1 -e GIT_CONFIG_KEY_0=safe.directory -e GIT_CONFIG_VALUE_0=/work \
        rust:1.92.0 ./regenerate.sh "$@"
fi

# Pinned so the wasm-pack build (step 2) is reproducible across machines; verified
# working on this machine. Bump deliberately, not implicitly via a stale cargo cache.
WASM_PACK_VERSION="0.15.0"
# SHA-256 of wasm-pack-v$WASM_PACK_VERSION-x86_64-unknown-linux-musl.tar.gz.
WASM_PACK_SHA256="c09f971ecaed9a2efc80fdcea7a00ef6b53c7fadc8c57d1f61b53a6aa66b668a"

# The vendoring target: an npm-shaped package directory, not a resources/
# path, because klib resources are not propagated to a consumer's linked
# output on Kotlin 2.4.20, whereas npm dependencies are.
PKG_DIR="../src/wasmJsMain/npm/hayagriva-wasm"

# --- 1. Toolchain -----------------------------------------------------------
# rust-toolchain.toml pins the channel; rustup reads it from cwd.
rustup target add wasm32-unknown-unknown

installed_wasm_pack_version=""
if command -v wasm-pack >/dev/null 2>&1; then
    installed_wasm_pack_version="$(wasm-pack --version | awk '{print $2}')"
fi
if [ "$installed_wasm_pack_version" != "$WASM_PACK_VERSION" ]; then
    # The prebuilt musl binary installs in seconds on x86_64 Linux (CI and
    # the container above); `cargo install` is the slow fallback.
    if [ "$(uname -m)" = "x86_64" ]; then
        tarball="wasm-pack-v$WASM_PACK_VERSION-x86_64-unknown-linux-musl"
        archive="$(mktemp)"
        curl -sSfL "https://github.com/rustwasm/wasm-pack/releases/download/v$WASM_PACK_VERSION/$tarball.tar.gz" -o "$archive"
        echo "$WASM_PACK_SHA256  $archive" | sha256sum -c --quiet -
        tar -xzf "$archive" -C "${CARGO_HOME:-$HOME/.cargo}/bin" --strip-components=1 "$tarball/wasm-pack"
        rm -f "$archive"
    else
        cargo install wasm-pack --version "$WASM_PACK_VERSION" --locked
    fi
fi

# --- 2. Build -----------------------------------------------------------
# rustc embeds absolute source paths into the binary; remap them to fixed
# virtual roots so the output is byte-identical across machines.
export RUSTFLAGS="${RUSTFLAGS:-} --remap-path-prefix=${CARGO_HOME:-$HOME/.cargo}=/cargo --remap-path-prefix=$PWD=/build"
# `--no-pack`: wasm-pack's generated package.json embeds the tool version
# and lacks the "type"/entry-point shape this package needs; step 3 writes
# a deterministic one.
# `--release` already runs `wasm-opt` when available.
wasm-pack build --target web --release --no-pack --out-name hayagriva_wasm

# --- 3. Assemble the vendored package ---------------------------------------
# Clean first, so files dropped from `pkg/` also disappear from the package.
rm -rf "$PKG_DIR"
mkdir -p "$PKG_DIR"

cp pkg/hayagriva_wasm.js pkg/hayagriva_wasm_bg.wasm "$PKG_DIR/"
# loader.mjs and loader.d.ts are hand-written source, only copied into place.
cp js/loader.mjs js/loader.d.ts "$PKG_DIR/"

# wasm-bindgen's .d.ts outputs are optional; vendor them when present.
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

# Fixed `files` ordering keeps package.json byte-stable across runs.
files_json="\"loader.mjs\", \"loader.d.ts\", \"hayagriva_wasm.js\", \"hayagriva_wasm_bg.wasm\""
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
  "types": "./loader.d.ts",
  "exports": {
    ".": {
      "types": "./loader.d.ts",
      "default": "./loader.mjs"
    }
  },
  "files": [$files_json]
}
EOF

echo "Vendored $PKG_DIR:"
echo "  $(du -h "$PKG_DIR/hayagriva_wasm_bg.wasm" | cut -f1)  hayagriva_wasm_bg.wasm"
