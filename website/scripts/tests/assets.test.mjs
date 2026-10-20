import assert from 'node:assert/strict';
import { mkdtemp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { copyPublicJavadocs, requireGeneratedAssets } from '../lib/assets.mjs';
import { createConsumerProject } from '../lib/consumer-project.mjs';
import { root as projectRoot } from '../project.mjs';

test('downloadable examples carry the project licence alongside the font licence', async () => {
  const destination = await mkdtemp(path.join(os.tmpdir(), 'letter-examples-'));
  try {
    await createConsumerProject(projectRoot, destination, '1.0.0');
    assert.equal(await readFile(path.join(destination, 'LICENSE'), 'utf8'),
      await readFile(path.join(projectRoot, 'LICENSE'), 'utf8'));
    assert.match(await readFile(path.join(destination, 'src/main/resources/fonts/LICENSE.txt'), 'utf8'), /DejaVu/);
  } finally { await rm(destination, { recursive: true, force: true }); }
});

test('missing Maven outputs fail with the preparation command and missing filenames', async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), 'letter-docs-'));
  try {
    await assert.rejects(requireGeneratedAssets(root), (error) => {
      assert.match(error.message, /mvnw clean verify/);
      assert.match(error.message, /quickstart-fluent.pdf/);
      assert.match(error.message, /apidocs/);
      return true;
    });
  } finally { await rm(root, { recursive: true, force: true }); }
});

test('public Javadocs reject implementation packages instead of publishing them', async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), 'letter-docs-'));
  try {
    const source = path.join(root, 'source');
    await mkdir(path.join(source, 'com/nsdev/lettergenerator/internal'), { recursive: true });
    await writeFile(path.join(source, 'index.html'), '<html>API</html>');
    await assert.rejects(copyPublicJavadocs(source, path.join(root, 'out')), /internal/);
  } finally { await rm(root, { recursive: true, force: true }); }
});

test('public Javadocs retain navigation assets and the entry page', async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), 'letter-docs-'));
  try {
    const source = path.join(root, 'source');
    await mkdir(source);
    await writeFile(path.join(source, 'index.html'), '<html>API</html>');
    await writeFile(path.join(source, 'stylesheet.css'), 'body{}');
    await mkdir(path.join(source, 'META-INF'));
    await writeFile(path.join(source, 'META-INF/LICENSE'), 'Project licence');
    await copyPublicJavadocs(source, path.join(root, 'out'));
    assert.equal(await readFile(path.join(root, 'out/index.html'), 'utf8'), '<html>API</html>');
    assert.equal(await readFile(path.join(root, 'out/stylesheet.css'), 'utf8'), 'body{}');
    assert.equal(await readFile(path.join(root, 'out/META-INF/LICENSE'), 'utf8'), 'Project licence');
  } finally { await rm(root, { recursive: true, force: true }); }
});

test('Java 17 constant-table package links point to an existing generated section', async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), 'letter-docs-'));
  try {
    const source = path.join(root, 'source');
    await mkdir(source);
    await writeFile(path.join(source, 'index.html'), '<html>API</html>');
    await writeFile(path.join(source, 'constant-values.html'), '<a href="#com.nsdev">com.nsdev.*</a><section class="constants-summary" id="com.nsdev.lettergenerator"></section>');
    await copyPublicJavadocs(source, path.join(root, 'out'));
    const output = await readFile(path.join(root, 'out/constant-values.html'), 'utf8');
    assert.match(output, /href="#com\.nsdev\.lettergenerator"/);
    assert.ok(!output.includes('href="#com.nsdev"'));
  } finally { await rm(root, { recursive: true, force: true }); }
});
