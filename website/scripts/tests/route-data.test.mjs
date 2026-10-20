import assert from 'node:assert/strict';
import test from 'node:test';
import { onRequest } from '../../src/route-data.ts';

test('hero actions use the configured base and ordinary pages retain canonical metadata', async () => {
  const route = { id: 'index', head: [{ tag: 'link', attrs: { rel: 'canonical', href: 'https://nunosilva-dev.github.io/letter-generator/' } }],
    entry: { data: { hero: { actions: [{ link: '/start/quickstart/' }, { link: 'https://example.com/' }] } } } };
  await onRequest({ locals: { starlightRoute: route } }, async () => {});
  assert.equal(route.entry.data.hero.actions[0].link, '/letter-generator/start/quickstart/');
  assert.equal(route.entry.data.hero.actions[1].link, 'https://example.com/');
  assert.equal(route.head[0].attrs.href, 'https://nunosilva-dev.github.io/letter-generator/');
});

test('404 metadata does not advertise a nonexistent canonical page', async () => {
  const route = { id: '404', entry: { data: {} }, head: [
    { tag: 'link', attrs: { rel: 'canonical', href: 'https://nunosilva-dev.github.io/letter-generator/404/' } },
    { tag: 'meta', attrs: { property: 'og:url', content: 'https://nunosilva-dev.github.io/letter-generator/404/' } },
    { tag: 'meta', attrs: { name: 'robots', content: 'noindex' } },
  ] };
  await onRequest({ locals: { starlightRoute: route } }, async () => {});
  assert.deepEqual(route.head, [{ tag: 'meta', attrs: { name: 'robots', content: 'noindex' } }]);
});
