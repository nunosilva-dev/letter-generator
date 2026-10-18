/**
 * Strict deserialization boundary for the JSON input path. {@link
 * com.nsdev.lettergenerator.internal.json.JsonInput} uses one privately configured,
 * read-only-after-initialization Jackson mapper to produce {@link
 * com.nsdev.lettergenerator.model.LetterDocument}. Unknown properties, duplicate keys, trailing
 * tokens and selected scalar coercions fail before rendering; mapping failures become {@link
 * com.nsdev.lettergenerator.exception.InvalidLetterException} with the closest available
 * field/index path. JSON parsing constructs the model; the shared validator subsequently checks
 * semantic validity, geometry, links, images and registered fonts.
 */
package com.nsdev.lettergenerator.internal.json;
