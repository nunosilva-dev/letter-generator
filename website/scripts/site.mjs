/** Hosting settings shared by Astro, authored links and the built-site checks. */
export const site = 'https://nunosilva-dev.github.io';
export const base = '/letter-generator';

/** Prefix a root-relative URL once; leave external, relative and fragment URLs unchanged. */
export function withBase(value, basePath = base) {
  if (!value.startsWith('/') || value.startsWith('//')) return value;
  const prefix = basePath.replace(/\/+$/, '');
  const pathname = value.split(/[?#]/, 1)[0];
  if (!prefix || pathname === prefix || pathname.startsWith(`${prefix}/`)) return value;
  return `${prefix}${value}`;
}
