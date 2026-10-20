import { readFile, readdir, stat } from 'node:fs/promises';
import path from 'node:path';
import { parseHTML } from 'linkedom';

export async function checkBuiltLinks(dist) {
  const htmlFiles = [];
  async function walk(directory) {
    for (const entry of await readdir(directory, { withFileTypes: true })) {
      const file = path.join(directory, entry.name);
      if (entry.isDirectory()) await walk(file);
      else if (entry.name.endsWith('.html')) htmlFiles.push(file);
    }
  }
  await walk(dist);
  const pages = new Map();
  async function documentOf(file) {
    if (!pages.has(file)) pages.set(file, parseHTML(await readFile(file, 'utf8')).document);
    return pages.get(file);
  }
  const failures = []; let checked = 0;
  for (const file of htmlFiles) {
    const document = await documentOf(file);
    const route = `/${path.relative(dist, file).split(path.sep).join('/')}`;
    for (const element of document.querySelectorAll('a[href], img[src], script[src], link[href], iframe[src], source[src]')) {
      const value = element.getAttribute(element.hasAttribute('href') ? 'href' : 'src');
      if (!value || /^(?:[a-z][\w+.-]*:|\/\/)/i.test(value)) continue;
      const url = new URL(value, `https://documentation.invalid${route}`);
      let target = path.join(dist, decodeURIComponent(url.pathname));
      try {
        if ((await stat(target)).isDirectory()) target = path.join(target, 'index.html');
        if (!(await stat(target)).isFile()) throw new Error('Not a file');
      } catch { failures.push(`${route}: missing ${value}`); continue; }
      checked++;
      if (url.hash && target.endsWith('.html')) {
        const fragment = decodeURIComponent(url.hash.slice(1));
        const targetDocument = await documentOf(target);
        if (fragment && !targetDocument.getElementById(fragment)
          && ![...targetDocument.querySelectorAll('[name]')].some((node) => node.getAttribute('name') === fragment)) {
          failures.push(`${route}: missing anchor ${value}`);
        }
      }
    }
  }
  return { pages: htmlFiles.length, checked, failures: [...new Set(failures)] };
}
