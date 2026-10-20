import { existsSync, readFileSync } from 'node:fs';
import path from 'node:path';

// Astro relocates server modules during prerendering. Locate the checkout from the
// working directory rather than interpreting the bundled module's file URL.
function repositoryRoot() {
  let directory = process.cwd();
  while (true) {
    const pom = path.join(directory, 'pom.xml');
    if (existsSync(pom) && readFileSync(pom, 'utf8').includes('<artifactId>letter-generator</artifactId>')) return directory;
    const parent = path.dirname(directory);
    if (parent === directory) throw new Error('Run the website commands from the library checkout or website directory.');
    directory = parent;
  }
}
export const root = repositoryRoot();
export const website = path.join(root, 'website');
export const version = readFileSync(path.join(root, 'pom.xml'), 'utf8').match(/<version>([^<]+)<\/version>/)[1];
