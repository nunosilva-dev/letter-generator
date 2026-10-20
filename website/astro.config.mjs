import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import { version } from './scripts/project.mjs';

export default defineConfig({
  output: 'static',
  integrations: [starlight({
    title: 'Letter Generator',
    description: 'The complete developer manual for creating PDF letters with Java, JSON and letter HTML.',
    favicon: '/favicon.svg',
    customCss: ['./src/styles/custom.css'],
    components: { SiteTitle: './src/components/SiteTitle.astro' },
    head: [{ tag: 'meta', attrs: { name: 'letter-generator-version', content: version } }],
    sidebar: [
      { label: 'Start here', items: [
        { label: 'Introduction', slug: 'start/introduction' },
        { label: 'Install the library', slug: 'start/installation' },
        { label: 'Choose an API', slug: 'start/choose-api' },
        { label: 'Your first letter', slug: 'start/quickstart' },
      ] },
      { label: 'API guides', items: [{ autogenerate: { directory: 'apis' } }] },
      { label: 'Letter features', items: [
        { label: 'Content and pagination', collapsed: true, items: [
          'guides/text-pagination', 'guides/styling', 'guides/tables-lists', 'guides/images', 'guides/layout-containers',
        ] },
        { label: 'Paper and stationery', collapsed: true, items: [
          'guides/page-layout', 'guides/semantic-parts', 'guides/headers', 'guides/footers',
          'guides/fixed-regions', 'guides/signatures', 'guides/watermarks', 'guides/company-stationery',
        ] },
        { label: 'Themes and template data', collapsed: true, items: [
          'guides/themes-components', 'guides/template-data', 'guides/prepared-templates',
        ] },
        { label: 'Languages and fonts', collapsed: true, items: [
          'guides/fonts', 'guides/localization', 'guides/writing-systems', 'guides/country-layouts',
        ] },
        { label: 'Rendering and integration', collapsed: true, items: [
          'guides/diagnostics', 'guides/output-profiles', 'guides/pdf-packs', 'guides/application-integration',
        ] },
      ] },
      { label: 'Reference', items: [{ autogenerate: { directory: 'reference' } }] },
      { label: 'Examples', items: [{ autogenerate: { directory: 'examples' } }] },
      { label: 'Support', items: [{ autogenerate: { directory: 'support' } }] },
      { label: 'Java API / Javadoc', link: '/api/' },
    ],
  })],
});
