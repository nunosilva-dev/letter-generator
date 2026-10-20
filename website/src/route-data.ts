import { defineRouteMiddleware } from '@astrojs/starlight/route-data';
import { withBase } from '../scripts/site.mjs';

/** Adjust frontmatter links and error-page metadata before Starlight renders a page. */
export const onRequest = defineRouteMiddleware(({ locals }, next) => {
  const route = locals.starlightRoute;
  for (const action of route.entry.data.hero?.actions ?? []) {
    action.link = withBase(action.link);
  }
  if (route.id === '404') {
    route.head = route.head.filter(({ tag, attrs }) =>
      !(tag === 'link' && attrs?.rel === 'canonical')
      && !(tag === 'meta' && attrs?.property === 'og:url'));
  }
  return next();
});
