import { cp, mkdir, readdir, readFile, stat, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { parseHTML } from 'linkedom';

export const examples = [
  'quickstart-typed', 'quickstart-fluent', 'quickstart-json', 'quickstart-html',
  'energy-reference-letter', 'energy-fluent-letter', 'energy-json-letter', 'energy-template-letter',
  'composition-features', 'composition-debug', 'v1-authoring', 'v1-layout-pack',
];

export async function requireGeneratedAssets(root) {
  const required = ['target/apidocs/index.html', 'target/examples/v1-layout-pack-2.png', 'target/examples/v1-layout-pack-3.png', ...examples.flatMap((name) =>
    [`target/examples/${name}.pdf`, `target/examples/${name}.png`])];
  const missing = [];
  for (const file of required) {
    try { if (!(await stat(path.join(root, file))).isFile()) missing.push(file); }
    catch { missing.push(file); }
  }
  if (missing.length) throw new Error(
    `Documentation assets are missing. From the repository root run ./mvnw clean verify, then retry.\n${missing.join('\n')}`);
}

export async function copyPublicJavadocs(source, destination) {
  async function check(directory) {
    for (const item of await readdir(directory, { withFileTypes: true })) {
      if (item.isSymbolicLink()) throw new Error(`Javadoc symlink is not publishable: ${item.name}`);
      if (item.isDirectory()) {
        if (item.name === 'internal') throw new Error('Refusing to publish internal Javadoc packages. Regenerate the public API site.');
        await check(path.join(directory, item.name));
      }
    }
  }
  await check(source);
  await mkdir(destination, { recursive: true });
  await cp(source, destination, {
    recursive: true,
    filter: async (file) => (await stat(file)).isDirectory()
      || /\.(html|js|css|png|gif|svg|woff2?|ttf|txt)$/.test(file)
      || ['element-list', 'package-list'].includes(path.basename(file)),
  });
  // Java 17's doclet can link a two-part package group to an absent anchor.
  // Correct only those generated TOC links for which a real section exists.
  const constantsFile = path.join(destination, 'constant-values.html');
  let constants;
  try { constants = await readFile(constantsFile, 'utf8'); }
  catch (error) { if (error.code === 'ENOENT') return; throw error; }
  const { document } = parseHTML(constants);
  const sectionIds = [...document.querySelectorAll('section.constants-summary[id]')]
    .map((section) => section.id);
  for (const link of document.querySelectorAll('a[href^="#"]')) {
    const fragment = link.getAttribute('href').slice(1);
    if (document.getElementById(fragment)) continue;
    const target = sectionIds.find((id) => id.startsWith(`${fragment}.`));
    if (target) constants = constants.replaceAll(`href="#${fragment}"`, `href="#${target}"`);
  }
  await writeFile(constantsFile, constants);
}
