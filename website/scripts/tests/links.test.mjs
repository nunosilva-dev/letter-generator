import assert from 'node:assert/strict';
import { mkdtemp, mkdir, writeFile, rm } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { checkBuiltLinks } from '../lib/links.mjs';

test('built links check decoded anchors, directory pages and downloads', async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), 'letter-links-'));
  try {
    await mkdir(path.join(root, 'guide'));
    await writeFile(path.join(root, 'index.html'), '<a href="/guide/#call%28%29">OK</a><a href="/guide/#missing">Bad</a><img src="/missing.png"><a href="https://example.com/">External</a><a href="/letter.pdf">PDF</a>');
    await writeFile(path.join(root, 'guide/index.html'), '<h2 id="call()">Contract</h2>');
    await writeFile(path.join(root, 'letter.pdf'), 'PDF');
    const report = await checkBuiltLinks(root);
    assert.equal(report.pages, 2);
    assert.equal(report.failures.length, 2);
    assert.ok(report.failures.some((failure) => failure.includes('#missing')));
    assert.ok(report.failures.some((failure) => failure.includes('/missing.png')));
    assert.ok(!report.failures.some((failure) => failure.includes('call%28')));
  } finally { await rm(root, { recursive: true, force: true }); }
});
