// Checks that the vendored hayagriva binding matches a fresh build of the
// crate by comparing rendered outputs over a fixture corpus. Byte-level
// codegen differences are ignored, so the check is stable across build hosts.
//
// Usage: node check-binding.mjs <committed-package-dir> <fresh-package-dir>
import { readFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';
import { resolve } from 'node:path';

const [committedDir, freshDir] = process.argv.slice(2);
if (!committedDir || !freshDir) {
    console.error('usage: node check-binding.mjs <committed-package-dir> <fresh-package-dir>');
    process.exit(2);
}

const BIBTEX = `
@article{einstein1905,
    author = {Einstein, Albert},
    title = {Zur Elektrodynamik bewegter K{\\"o}rper},
    journal = {Annalen der Physik},
    pages = {891--921},
    year = {1905}
}
@book{hawking1988,
    author = {Hawking, Stephen},
    title = {A Brief History of Time},
    year = {1988}
}
`;

const CSL_JSON = JSON.stringify([
    {
        id: 'doe2000',
        type: 'book',
        title: 'A \\ Backslash "Quoted" Title',
        author: [{ family: 'Doe', given: 'Jane' }],
        issued: { 'date-parts': [[2000]] },
    },
]);

async function loadPackage(dir) {
    const module = await import(pathToFileURL(resolve(dir, 'loader.mjs')).href);
    const manifest = JSON.parse(await readFile(resolve(dir, 'package.json'), 'utf8'));
    const loader = await readFile(resolve(dir, 'loader.mjs'), 'utf8');
    const loaderTypes = await readFile(resolve(dir, 'loader.d.ts'), 'utf8');
    return { Bibliographer: module.Bibliographer, manifest, loader, loaderTypes };
}

// Every output an engine produces for the corpus, keyed by case name.
// Failures are captured as values, so error paths are compared too.
function corpus(Bibliographer) {
    const out = {};
    const run = (name, render) => {
        try {
            out[name] = render();
        } catch (error) {
            out[name] = `throws: ${error?.message ?? error}`;
        }
    };

    run('ieee.keys', () => new Bibliographer('ieee', BIBTEX, 'bibtex', undefined).citation_keys());
    run('ieee.citation.single', () => new Bibliographer('ieee', BIBTEX, 'bibtex', undefined).citation('["einstein1905"]'));
    run('ieee.citation.pair', () => new Bibliographer('ieee', BIBTEX, 'bibtex', undefined).citation('["einstein1905","hawking1988"]'));
    run('ieee.bibliography', () => new Bibliographer('ieee', BIBTEX, 'bibtex', undefined).bibliography());
    run('ieee.locale.de', () => new Bibliographer('ieee', BIBTEX, 'bibtex', 'de-DE').bibliography());
    run('apa.citation', () => new Bibliographer('apa', BIBTEX, 'bibtex', undefined).citation('["einstein1905"]'));
    run('apa.bibliography', () => new Bibliographer('apa', BIBTEX, 'bibtex', undefined).bibliography());
    run('cslJson.keys', () => new Bibliographer('ieee', CSL_JSON, 'csl-json', undefined).citation_keys());
    run('cslJson.bibliography', () => new Bibliographer('ieee', CSL_JSON, 'csl-json', undefined).bibliography());
    run('style.byZoteroId', () => new Bibliographer('chicago-notes-bibliography', BIBTEX, 'bibtex', undefined).bibliography());
    run('style.unknown', () => new Bibliographer('not-a-style', BIBTEX, 'bibtex', undefined).bibliography());
    run('source.invalid', () => new Bibliographer('ieee', 'not bibtex {{{', 'bibtex', undefined).bibliography());
    run('format.unsupported', () => new Bibliographer('ieee', BIBTEX, 'ris', undefined).bibliography());
    return out;
}

const committed = await loadPackage(committedDir);
const fresh = await loadPackage(freshDir);

const mismatches = [];

if (committed.manifest.version !== fresh.manifest.version) {
    mismatches.push(`package version: committed ${committed.manifest.version}, fresh ${fresh.manifest.version}`);
}
if (committed.loader !== fresh.loader) {
    mismatches.push('loader.mjs: committed copy differs from source');
}
if (committed.loaderTypes !== fresh.loaderTypes) {
    mismatches.push('loader.d.ts: committed copy differs from source');
}

const committedOut = corpus(committed.Bibliographer);
const freshOut = corpus(fresh.Bibliographer);
for (const name of Object.keys(freshOut)) {
    if (committedOut[name] !== freshOut[name]) {
        mismatches.push(`${name}:\n  committed: ${committedOut[name]}\n  fresh:     ${freshOut[name]}`);
    }
}

if (mismatches.length > 0) {
    console.error('::error::Vendored hayagriva binding is out of date. Run hayagriva-wasm/regenerate.sh and commit the result.');
    for (const mismatch of mismatches) console.error(mismatch);
    process.exit(1);
}
console.log(`Vendored binding is functionally current (${Object.keys(freshOut).length} cases).`);
