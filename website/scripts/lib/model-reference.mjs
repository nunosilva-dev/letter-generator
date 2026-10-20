import { readFile, readdir } from 'node:fs/promises';
import path from 'node:path';

function cleanDoc(text) {
  return text.replace(/\{@(?:link|linkplain)\s+([^}]+)\}/g, (_, value) => value.trim().split(/\s+/).at(-1))
    .replace(/\{@code\s+([^}]+)\}/g, '$1').replace(/<[^>]*>/g, '')
    .replace(/\s+/g, ' ').trim();
}
function fieldsOf(header) {
  const result = []; let depth = 0; let start = 0;
  for (let i = 0; i <= header.length; i++) {
    if (header[i] === '<') depth++;
    if (header[i] === '>') depth--;
    if (i === header.length || header[i] === ',' && depth === 0) {
      const field = header.slice(start, i).trim().match(/^(.+?)\s+(\w+)$/s);
      if (field) result.push({ name: field[2], type: field[1].replace(/\s+/g, ' ') });
      start = i + 1;
    }
  }
  return result;
}
export async function readModelReference(sourceRoot) {
  const records = new Map(); const enums = new Map(); const polymorphic = new Map();
  enums.set('FormatStyle', {
    name: 'FormatStyle', packageName: 'java.time.format',
    values: ['FULL', 'LONG', 'MEDIUM', 'SHORT'],
    javadoc: 'https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/FormatStyle.html',
  });
  async function visit(directory) {
    for (const entry of await readdir(directory, { withFileTypes: true })) {
      if (entry.name === 'internal') continue;
      const file = path.join(directory, entry.name);
      if (entry.isDirectory()) { await visit(file); continue; }
      if (!entry.name.endsWith('.java')) continue;
      const source = await readFile(file, 'utf8');
      const packageName = source.match(/^package ([\w.]+);/m)?.[1];
      const record = source.match(/public record (\w+)\s*\(([^)]*)\)/s);
      const enumType = source.match(/public enum (\w+)\s*\{([^;]*)(?:;|\})/s);
      if (record) {
        const docs = [...source.slice(0, record.index).matchAll(/\/\*\*([\s\S]*?)\*\//g)].at(-1)?.[1] || '';
        const normalized = docs.replace(/^\s*\* ?/gm, '');
        const descriptions = Object.fromEntries([...normalized.matchAll(/@param\s+(\w+)\s+([\s\S]*?)(?=\n@\w+|$)/g)]
          .map((m) => [m[1], cleanDoc(m[2])]));
        records.set(record[1], { name: record[1], packageName,
          description: cleanDoc(normalized.split(/\n@/)[0]),
          fields: fieldsOf(record[2]).map((field) => ({ ...field, description: descriptions[field.name] || '' })) });
      }
      if (enumType) {
        const uncommented = enumType[2].replace(/\/\*[\s\S]*?\*\//g, '');
        enums.set(enumType[1], { name: enumType[1], packageName,
          values: [...uncommented.matchAll(/^\s*([A-Z][A-Z_0-9]*)\s*(?=[,(}]|$)/gm)].map((m) => m[1]) });
      }
      if (source.includes('@JsonSubTypes')) {
        const name = source.match(/public sealed interface (\w+)/)?.[1];
        const property = source.match(/@JsonTypeInfo\([^\n]*property = "([^"]+)"/)?.[1];
        if (name) polymorphic.set(name, { name, property, variants: [...source.matchAll(/@JsonSubTypes.Type\(value = (\w+)\.class, name = "([^"]+)"\)/g)]
          .map((m) => ({ name: m[1], value: m[2] })) });
      }
    }
  }
  await visit(sourceRoot);
  const visited = new Set(); const queue = ['LetterDocument'];
  while (queue.length) {
    const name = queue.shift(); if (visited.has(name)) continue; visited.add(name);
    if (records.has(name)) for (const field of records.get(name).fields) queue.push(...field.type.match(/\b[A-Z]\w*\b/g) || []);
    if (polymorphic.has(name)) queue.push(...polymorphic.get(name).variants.map((variant) => variant.name));
  }
  const scalarsAndContainers = new Set(['String', 'Boolean', 'Double', 'Integer', 'Long', 'Float', 'Short', 'Byte', 'Character', 'List', 'Map']);
  for (const name of visited) {
    if (!records.has(name) && !enums.has(name) && !polymorphic.has(name) && !scalarsAndContainers.has(name)) {
      throw new Error(`Undocumented referenced JSON type: ${name}. Add its public contract before publishing the reference.`);
    }
  }
  const selected = (map) => [...map.values()].filter((item) => visited.has(item.name)).sort((a,b) => a.name.localeCompare(b.name));
  return { records: selected(records), enums: selected(enums), polymorphic: selected(polymorphic) };
}
