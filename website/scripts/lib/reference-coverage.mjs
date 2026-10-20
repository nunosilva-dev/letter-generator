import { readFile, readdir } from 'node:fs/promises';
import path from 'node:path';
import { readModelReference } from './model-reference.mjs';

const quoted = (source) => [...source.matchAll(/"([^"\n]+)"/g)].map((match) => match[1]);
function calls(source, name) {
  const result = []; let from = 0;
  while ((from = source.indexOf(`${name}(`, from)) >= 0) {
    const start = from + name.length + 1; let depth = 1; let inString = false; let escaped = false;
    let cursor = start;
    for (; cursor < source.length && depth; cursor++) {
      const character = source[cursor];
      if (escaped) { escaped = false; continue; }
      if (character === '\\' && inString) { escaped = true; continue; }
      if (character === '"') { inString = !inString; continue; }
      if (!inString && character === '(') depth++;
      if (!inString && character === ')') depth--;
    }
    result.push(source.slice(start, cursor - 1)); from = cursor;
  }
  return result;
}
export async function implementationVocabulary(sourceRoot) {
  const read = (file) => readFile(path.join(sourceRoot, 'internal', file), 'utf8');
  const blocks = await read('markup/HtmlBlocks.java');
  const inline = await read('markup/InlineReader.java');
  const styles = await read('markup/HtmlStyles.java');
  const layouts = await read('markup/HtmlLayouts.java');
  const tables = await read('markup/HtmlTables.java');
  const tags = new Set(['letter', 'thead', 'tbody', 'tr', 'th', 'td', 'li', 'column', 'if', 'else', 'param']);
  for (const source of [blocks, inline]) {
    for (const set of source.matchAll(/(?:TEXT_BLOCKS|INLINE_TAGS|SUPPORTED_TAGS|INLINE|TAGS)\s*=\s*Set.of\(([^;]+)\);/gs)) quoted(set[1]).forEach((tag) => tags.add(tag));
    for (const branch of source.matchAll(/case ((?:"[^"]+"\s*,?\s*)+)\s*->/g)) quoted(branch[1]).forEach((tag) => tags.add(tag));
  }
  const attributes = new Set(['style', 'style-name', 'data-if', 'data-each', 'data-as']);
  for (const directory of ['template', 'markup']) {
    for (const file of await readdir(path.join(sourceRoot, 'internal', directory))) {
      if (!file.endsWith('.java')) continue;
      const source = await read(`${directory}/${file}`);
      for (const match of source.matchAll(/String tagName\(\)\s*\{\s*return "([^"]+)";/g)) tags.add(match[1]);
      for (const name of ['MarkupSupport.attributes', 'TemplateBlocks.read']) {
        for (const call of calls(source, name)) quoted(call).forEach((attribute) => attributes.add(attribute));
      }
    }
  }
  const semantic = await readFile(path.join(sourceRoot, 'letter/LetterElement.java'), 'utf8');
  for (const match of semantic.matchAll(/^\s*([A-Z_]+)\s*(?:[,;]|$)/gm)) tags.add(match[1].toLowerCase());
  const css = new Set();
  for (const source of [styles.slice(styles.indexOf('public static Appearance read'), styles.indexOf('private static Keep keep')),
    tables.slice(tables.indexOf('static ParsedAppearance appearance'), tables.indexOf('private static Integer span'))]) {
    for (const match of source.matchAll(/case "([^"]+)"\s*->/g)) css.add(match[1]);
  }
  for (const name of ['border-top', 'border-right', 'border-bottom', 'border-left']) {
    if (layouts.includes(`"${name}"`)) css.add(name);
  }
  return { tags: [...tags].sort(), attributes: [...attributes].sort(), css: [...css].sort() };
}
export function validateCatalogue(catalogue, vocabulary) {
  const documented = {
    tags: new Set(catalogue.tags.map((tag) => tag.name)),
    css: new Set(catalogue.css.map((property) => property.name)),
    attributes: new Set([...Object.keys(catalogue.commonAttributes), ...catalogue.tags.flatMap((tag) => Object.keys(tag.attributes))]),
  };
  const failures = [];
  for (const kind of ['tags', 'css', 'attributes']) {
    for (const name of vocabulary[kind]) if (!documented[kind].has(name)) failures.push(`Undocumented ${kind}: ${name}`);
    for (const name of documented[kind]) if (!vocabulary[kind].includes(name)) failures.push(`Unsupported documented ${kind}: ${name}`);
  }
  for (const tag of catalogue.tags) {
    for (const property of ['context', 'defaults', 'description', 'example']) if (!tag[property]?.trim()) failures.push(`Incomplete ${tag.name}: ${property}`);
  }
  return failures;
}
export async function checkReference(root) {
  const sourceRoot = path.join(root, 'src/main/java/com/nsdev/lettergenerator');
  const catalogue = JSON.parse(await readFile(path.join(root, 'website/src/data/markup-reference.json'), 'utf8'));
  const vocabulary = await implementationVocabulary(sourceRoot);
  const failures = validateCatalogue(catalogue, vocabulary);
  const model = await readModelReference(sourceRoot);
  for (const record of model.records) for (const field of record.fields) if (!field.description) failures.push(`Undocumented JSON field: ${record.name}.${field.name}`);
  if (!model.polymorphic.find((type) => type.name === 'Block')?.variants.length) failures.push('Missing JSON block discriminators');
  if (failures.length) throw new Error(failures.join('\n'));
  return { tags: vocabulary.tags.length, css: vocabulary.css.length, attributes: vocabulary.attributes.length, objects: model.records.length };
}
