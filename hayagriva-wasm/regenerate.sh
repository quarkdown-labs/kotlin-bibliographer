#!/usr/bin/env bash
# Rebuilds the wasm binary and re-vendors the npm-shaped `hayagriva-wasm`
# package consumed by the Kotlin/JS (wasmJs) target.
#
# Non-interactive. Output is meant to be byte-identical across runs and
# machines: a CI drift check diffs the vendored package directory against
# what is committed, so nothing here may embed a timestamp, absolute path,
# or other non-reproducible value.
set -euo pipefail
cd "$(dirname "$0")"

# Linux is the authoritative build platform: rustc's output is deterministic
# per host platform but not across platforms (constant ordering differs
# between e.g. macOS-arm64 and Linux-x64 hosts), and the CI drift check
# rebuilds on Linux. On any other system, delegate the whole run to a pinned
# Linux container so local regeneration produces the exact bytes CI expects.
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

# `--check` additionally verifies (after regenerating) that the vendored
# package matches what is committed — the CI drift check. Keeping the check
# here, next to PKG_DIR, means the workflow never hardcodes the package path.
CHECK=false
if [ "${1:-}" = "--check" ]; then
    CHECK=true
fi

# Pinned so the wasm-pack build (step 2) is reproducible across machines; verified
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
    # The prebuilt musl binary installs in seconds where available (Linux
    # x86_64 — both CI and the container above); `cargo install` compiles
    # for minutes and is only the fallback.
    if [ "$(uname -m)" = "x86_64" ]; then
        tarball="wasm-pack-v$WASM_PACK_VERSION-x86_64-unknown-linux-musl"
        curl -sSfL "https://github.com/rustwasm/wasm-pack/releases/download/v$WASM_PACK_VERSION/$tarball.tar.gz" |
            tar -xz -C "${CARGO_HOME:-$HOME/.cargo}/bin" --strip-components=1 "$tarball/wasm-pack"
    else
        cargo install wasm-pack --version "$WASM_PACK_VERSION" --locked
    fi
fi

# --- 2. Build -----------------------------------------------------------
# rustc embeds absolute source paths into the binary; remap them to fixed
# virtual roots so the output is byte-identical across machines.
export RUSTFLAGS="${RUSTFLAGS:-} --remap-path-prefix=${CARGO_HOME:-$HOME/.cargo}=/cargo --remap-path-prefix=$PWD=/build"
# `--no-pack` skips wasm-pack's own package.json generation: that output
# embeds the installed wasm-pack version and isn't shaped the way we need
# (no "type": "module", no loader.mjs entry point), so it would both be
# non-deterministic across machines and wrong. Step 4 writes our own
# deterministic package.json instead.
# `--release` already runs `wasm-opt` when available.
wasm-pack build --target web --release --no-pack --out-name hayagriva_wasm

# --- 3. Assemble the vendored package ---------------------------------------
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
