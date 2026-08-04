/**
 * Mermaid diagram validator.
 *
 * Every ```mermaid fence in the maintained docs is parsed with the real Mermaid
 * parser. An invalid diagram renders on GitHub as a raw error block — visible to
 * everyone, noticed by nobody — so this turns "validate it before committing"
 * from an instruction into a check.
 *
 * Companion to scripts/check_docs.py, which validates the docs' *claims*; this
 * validates their *pictures*. Needs Node + mermaid + jsdom, which is why it is a
 * separate step: check_docs.py stays dependency-free and always runnable.
 *
 *     npm install --no-save mermaid jsdom && node scripts/check_mermaid.mjs
 *
 * Skips docs/archive/ — historical documents are never updated (see
 * docs/DOCUMENTATION.md §6).
 */

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { JSDOM } from 'jsdom';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const SKIP_DIRS = new Set(['node_modules', '.git', 'archive']);

// Mermaid's parser reaches for a DOM even when only parsing.
const dom = new JSDOM('<!doctype html><html><body></body></html>', { pretendToBeVisual: true });
global.window = dom.window;
global.document = dom.window.document;
Object.defineProperty(global, 'navigator', { value: dom.window.navigator, configurable: true });
global.Element = dom.window.Element;
global.SVGElement = dom.window.SVGElement;
global.Node = dom.window.Node;
global.HTMLElement = dom.window.HTMLElement;
global.getComputedStyle = dom.window.getComputedStyle;
global.requestAnimationFrame = (cb) => setTimeout(cb, 0);

const mermaid = (await import('mermaid')).default;
mermaid.initialize({ startOnLoad: false });

function markdownFiles(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (!SKIP_DIRS.has(entry.name)) markdownFiles(full, out);
    } else if (entry.name.endsWith('.md')) {
      out.push(full);
    }
  }
  return out;
}

const files = [
  ...markdownFiles(path.join(ROOT, 'docs')),
  path.join(ROOT, 'README.md'),
  path.join(ROOT, 'CLAUDE.md'),
];

let total = 0;
const failures = [];

for (const file of files) {
  const text = fs.readFileSync(file, 'utf8');
  const fence = /```mermaid\n([\s\S]*?)```/g;
  let match;
  while ((match = fence.exec(text)) !== null) {
    total += 1;
    const line = text.slice(0, match.index).split('\n').length;
    try {
      await mermaid.parse(match[1]);
    } catch (error) {
      const detail = String(error?.message ?? error).split('\n').slice(0, 4).join('\n    ');
      failures.push(`  FAIL ${path.relative(ROOT, file)}:${line}\n    ${detail}`);
    }
  }
}

for (const failure of failures) console.log(failure);
console.log();
if (failures.length) {
  console.log(`${failures.length} of ${total} mermaid diagrams are invalid.`);
  console.log('An invalid diagram renders as a raw error block on GitHub. Fix the syntax —');
  console.log('see docs/DOCUMENTATION.md §3 for the diagram standard.');
  process.exit(1);
}
console.log(`All ${total} mermaid diagrams parse.`);
