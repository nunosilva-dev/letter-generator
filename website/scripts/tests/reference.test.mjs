import assert from 'node:assert/strict';
import test from 'node:test';
import { validateCatalogue, checkReference } from '../lib/reference-coverage.mjs';
import { root } from '../project.mjs';

test('reference coverage reports newly supported tags and removed properties', () => {
  const catalogue = { tags: [], css: [{ name: 'position' }], commonAttributes: {} };
  const failures = validateCatalogue(catalogue, { tags: ['canvas'], css: [], attributes: [] });
  assert.ok(failures.includes('Undocumented tags: canvas'));
  assert.ok(failures.includes('Unsupported documented css: position'));
});
test('the reference documents the current parser vocabulary and JSON fields', async () => {
  const counts = await checkReference(root);
  assert.ok(counts.tags >= 50);
  assert.ok(counts.objects >= 30);
});

test('the reachable JSON model includes external JDK enum values', async () => {
  const { readModelReference } = await import('../lib/model-reference.mjs');
  const { default: path } = await import('node:path');
  const model = await readModelReference(path.join(root, 'src/main/java/com/nsdev/lettergenerator'));
  const dateStyle = model.enums.find((type) => type.name === 'FormatStyle');
  assert.deepEqual(dateStyle?.values, ['FULL', 'LONG', 'MEDIUM', 'SHORT']);
  assert.match(dateStyle.javadoc, /^https:\/\/docs\.oracle\.com/);
});

test('a newly referenced opaque type fails reference generation instead of disappearing', async () => {
  const { readModelReference } = await import('../lib/model-reference.mjs');
  const { mkdtemp, writeFile, rm } = await import('node:fs/promises');
  const { default: os } = await import('node:os');
  const { default: path } = await import('node:path');
  const directory = await mkdtemp(path.join(os.tmpdir(), 'letter-model-'));
  try {
    await writeFile(path.join(directory, 'LetterDocument.java'), 'package example; public record LetterDocument(UnknownValue value) {}');
    await assert.rejects(readModelReference(directory), /Undocumented referenced JSON type: UnknownValue/);
  } finally { await rm(directory, { recursive: true, force: true }); }
});
