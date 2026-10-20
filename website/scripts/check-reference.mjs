import { root } from './project.mjs';
import { checkReference } from './lib/reference-coverage.mjs';
console.log('Reference matches implementation:', await checkReference(root));
