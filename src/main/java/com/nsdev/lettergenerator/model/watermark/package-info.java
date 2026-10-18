/**
 * Optional text and image decorations centered behind the physical page content. Watermarks select
 * pages without reserving layout space or changing pagination. JSON uses {@code kind} to
 * distinguish text and image variants. Canonical constructors preserve null appearance properties
 * for generation-time validation; subtype factories provide complete defaults. Font size uses
 * points, image dimensions millimeters, opacity spans zero to one, and rotation uses degrees about
 * page center.
 */
package com.nsdev.lettergenerator.model.watermark;
