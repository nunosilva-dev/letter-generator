import { cp, mkdir, rm } from 'node:fs/promises';
import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { root, website, version } from './project.mjs';
import { examples, copyPublicJavadocs, requireGeneratedAssets } from './lib/assets.mjs';
import { createConsumerProject } from './lib/consumer-project.mjs';

await requireGeneratedAssets(root);
const publicRoot = path.join(website, 'public');
await cp(path.join(root, 'LICENSE'), path.join(publicRoot, 'LICENSE.txt'));
for (const name of ['api', 'examples', 'downloads']) {
  await rm(path.join(publicRoot, name), { recursive: true, force: true });
  await mkdir(path.join(publicRoot, name), { recursive: true });
}
await copyPublicJavadocs(path.join(root, 'target/apidocs'), path.join(publicRoot, 'api'));
for (const name of examples) for (const extension of ['pdf', 'png']) {
  await cp(path.join(root, `target/examples/${name}.${extension}`), path.join(publicRoot, `examples/${name}.${extension}`));
}
for (const page of [2,3]) await cp(path.join(root, `target/examples/v1-layout-pack-${page}.png`), path.join(publicRoot, `examples/v1-layout-pack-${page}.png`));
await cp(path.join(root, 'src/test/resources/documentation'), path.join(publicRoot, 'downloads/quickstarts'), { recursive: true });
await cp(path.join(root, 'src/test/resources/letters/energy'), path.join(publicRoot, 'downloads/energy'), { recursive: true });
const consumer = path.join(root, 'target/documentation/consumer');
await rm(consumer, { recursive: true, force: true });
await createConsumerProject(root, consumer, version);
const jar = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin/jar') : 'jar';
execFileSync(jar, ['--create', '--no-manifest', '--file', path.join(publicRoot, 'downloads/letter-generator-examples.zip'), '-C', consumer, '.'], { stdio: 'inherit' });
console.log(`Prepared ${examples.length} tested examples, public Javadocs and the standalone consumer project (${version}).`);
