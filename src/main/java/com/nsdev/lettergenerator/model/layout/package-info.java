/**
 * Physical-page geometry and placement rules. Dimensions are in millimeters; bounds use the
 * upper-left corner of their containing zone, with x increasing rightward and y downward. Outer
 * margins enclose top, body, and bottom regions; a last-page signature zone sits above the bottom
 * region and reduces body height on the final page.
 *
 * <p>{@link com.nsdev.lettergenerator.model.layout.FixedElement} repeats positioned content
 * according to {@link com.nsdev.lettergenerator.model.layout.PageSelector}. A {@link
 * com.nsdev.lettergenerator.model.layout.Signature} either follows body flow or occupies bounds in
 * the reserved final-page zone. Geometry is validated during document generation.
 */
package com.nsdev.lettergenerator.model.layout;
