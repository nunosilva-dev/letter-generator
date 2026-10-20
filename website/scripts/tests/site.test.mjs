import assert from 'node:assert/strict';
import test from 'node:test';
import { withBase } from '../site.mjs';
import { createSatteriMarkdownProcessor } from '@astrojs/markdown-satteri';
import { basePathPlugin } from '../lib/base-path-plugin.mjs';

test('site URLs prefix local paths exactly once and preserve other references', () => {
  for (const [input, expected] of [
    ['/', '/letter-generator/'],
    ['/guides/headers/#position', '/letter-generator/guides/headers/#position'],
    ['/downloads/example.zip?download=1', '/letter-generator/downloads/example.zip?download=1'],
    ['/letter-generator/api/', '/letter-generator/api/'],
    ['/letter-generator?view=all', '/letter-generator?view=all'],
    ['/letter-generator-other/', '/letter-generator/letter-generator-other/'],
    ['#example', '#example'],
    ['../guide/', '../guide/'],
    ['https://example.com/api/', 'https://example.com/api/'],
    ['//example.com/image.png', '//example.com/image.png'],
  ]) assert.equal(withBase(input, '/letter-generator/'), expected);
  assert.equal(withBase('/guides/headers/', '/'), '/guides/headers/');
});

test('native Markdown rendering prefixes links, images and references without changing code', async () => {
  const renderer = await createSatteriMarkdownProcessor({
    syntaxHighlight: false,
    mdastPlugins: [basePathPlugin({ base: '/letter-generator' })],
  });
  const { code } = await renderer.render('[Guide](/guides/headers/)\n\n![Letter](/examples/letter.png)\n\n[PDF][pdf]\n\n[pdf]: /downloads/letter.pdf\n\n```html\n<a href="/literal-example/">Example</a>\n```');
  assert.ok(code.includes('href="/letter-generator/guides/headers/"'));
  assert.ok(code.includes('src="/letter-generator/examples/letter.png"'));
  assert.ok(code.includes('href="/letter-generator/downloads/letter.pdf"'));
  assert.ok(code.includes('/literal-example/'));
  assert.ok(!code.includes('/letter-generator/literal-example/'));
});

test('MDX component visitors prefix URL attributes without changing ordinary attributes', () => {
  const node = { type: 'mdxJsxFlowElement', name: 'LinkCard', attributes: [
      { type: 'mdxJsxAttribute', name: 'href', value: '/start/quickstart/' },
      { type: 'mdxJsxAttribute', name: 'title', value: '/literal title' },
    ], children: [] };
  const result = basePathPlugin({ base: '/letter-generator' }).mdxJsxFlowElement(node);
  assert.equal(result.attributes[0].value, '/letter-generator/start/quickstart/');
  assert.equal(result.attributes[1].value, '/literal title');
});
