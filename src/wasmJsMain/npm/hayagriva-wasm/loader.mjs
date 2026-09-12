// Top-level-await init shim for the hayagriva-wasm package.
//
// wasm-bindgen's `--target web` output requires the caller to fetch the
// `.wasm` binary and pass it (or a fetch Response/URL) to `init()` before
// the module's exports are usable. This file does that once, at module
// load time via top-level await, so consumers of the package can simply
// `import { Bibliographer } from 'hayagriva-wasm'` and start using the
// class immediately, with no async bootstrapping of their own.
//
// The wasm binary is located relative to this file via `import.meta.url`,
// so the package works wherever it is installed or imported from.
//
// Two runtimes are supported:
//   - Node.js: `fetch()` cannot load `file:` URLs on all supported Node
//     versions, so the wasm bytes are read directly from disk and handed
//     to `init()` as a buffer.
//   - Browsers (and other environments with a working `fetch`): the URL is
//     passed straight to `init()`, which streams/fetches it itself.
import init, { Bibliographer } from './hayagriva_wasm.js';

const wasmUrl = new URL('./hayagriva_wasm_bg.wasm', import.meta.url);

// wasm-bindgen 0.2.100+ deprecates `init()`'s positional-argument form.
if (typeof process !== 'undefined' && process.versions?.node) {
    const { readFile } = await import('node:fs/promises');
    const { fileURLToPath } = await import('node:url');
    await init({ module_or_path: await readFile(fileURLToPath(wasmUrl)) });
} else {
    await init({ module_or_path: wasmUrl });
}

export { Bibliographer };
export default Bibliographer;
