import { readFileSync } from 'node:fs';
import path from 'node:path';
import { root } from '../../scripts/project.mjs';
const source = (file) => readFileSync(path.join(root, file), 'utf8');
export const quickstarts = {
  fluent: source('src/test/resources/documentation/java/FluentQuickstart.java'),
  typed: source('src/test/resources/documentation/java/TypedQuickstart.java'),
  json: source('src/test/resources/documentation/letters/hello.json'),
  html: source('src/test/resources/documentation/letters/hello.html'),
};
export const runners = {
  json: source('src/test/resources/documentation/java/JsonQuickstart.java'),
  html: source('src/test/resources/documentation/java/HtmlQuickstart.java'),
};
export const energySources = {
  fluent: source('src/test/java/com/nsdev/lettergenerator/fixtures/FluentEnergyLetter.java'),
  typed: source('src/test/java/com/nsdev/lettergenerator/fixtures/EnergyReferenceLetter.java'),
  json: source('src/test/resources/letters/energy/energy-letter.json'),
  html: source('src/test/resources/letters/energy/energy-price-update.html'),
  values: source('src/test/resources/letters/energy/energy-values.json'),
};
