import { withBase } from '../site.mjs';

/** Native Sätteri visitors keep authored links portable and leave code examples untouched. */
export function basePathPlugin({ base } = {}) {
  const link = (node) => ({ ...node, url: withBase(node.url, base) });
  const component = (node) => ({
    ...node,
    attributes: node.attributes.map((attribute) =>
      ['href', 'src'].includes(attribute.name) && typeof attribute.value === 'string'
        ? { ...attribute, value: withBase(attribute.value, base) }
        : attribute),
  });
  return {
    name: 'documentation-base-paths',
    link,
    image: link,
    definition: link,
    mdxJsxFlowElement: component,
    mdxJsxTextElement: component,
  };
}
