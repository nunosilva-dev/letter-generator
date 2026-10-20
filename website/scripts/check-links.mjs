import path from 'node:path';
import { website } from './project.mjs';
import { checkBuiltLinks } from './lib/links.mjs';
const report = await checkBuiltLinks(path.join(website, 'dist'));
if (report.failures.length) throw new Error(report.failures.join('\n'));
console.log(`Verified ${report.checked} local links/assets/anchors across ${report.pages} generated HTML pages.`);
