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

test('project-site links resolve under the base, including relative Javadoc links', async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), 'letter-links-'));
  try {
    await mkdir(path.join(root, 'guide'));
    await mkdir(path.join(root, 'api'));
    await writeFile(path.join(root, 'index.html'), '<a href="/letter-generator/guide/#contract">Guide</a><a href="https://nunosilva-dev.github.io/letter-generator/api/">API</a><a href="/letter-generator/letter.pdf">PDF</a>');
    await writeFile(path.join(root, 'guide/index.html'), '<h2 id="contract">Contract</h2><a href="../api/">API</a>');
    await writeFile(path.join(root, 'api/index.html'), '<a href="package-summary.html">Package</a>');
    await writeFile(path.join(root, 'api/package-summary.html'), '<a href="../guide/#contract">Guide</a>');
    await writeFile(path.join(root, 'letter.pdf'), 'PDF');
    const report = await checkBuiltLinks(root, { base: '/letter-generator', site: 'https://nunosilva-dev.github.io' });
    assert.equal(report.pages, 4);
    assert.deepEqual(report.failures, []);
  } finally { await rm(root, { recursive: true, force: true }); }
});

test('project-site checks reject root links and relative paths escaping the deployment base', async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), 'letter-links-'));
  try {
    await mkdir(path.join(root, 'guide'));
    await writeFile(path.join(root, 'index.html'), '<a href="/guide/">Wrong root</a><a href="/letter-generator-other/">Wrong prefix</a>');
    await writeFile(path.join(root, 'guide/index.html'), '<a href="../../index.html">Escapes base</a>');
    const report = await checkBuiltLinks(root, { base: '/letter-generator' });
    assert.equal(report.failures.length, 3);
    assert.ok(report.failures.every((failure) => failure.includes('outside deployment base')));
  } finally { await rm(root, { recursive: true, force: true }); }
});
