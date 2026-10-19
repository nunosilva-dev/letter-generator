package com.nsdev.lettergenerator.internal.composition;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import com.nsdev.lettergenerator.model.layout.PageSpec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Merges document-local stationery by stable region identifier before content composition. */
final class StationeryPreparation {
    /** Prevents instances. */
    private StationeryPreparation() {}

    /**
     * Adds inherited regions only where the letter has no matching region identity.
     *
     * @param document immutable source
     * @return merged immutable source, or the unchanged legacy model
     */
    static LetterDocument prepare(LetterDocument document) {
        if (document.theme() == null || document.theme().stationery() == null) {
            return document;
        }
        var stationery = document.theme().stationery();
        if (!Double.isFinite(stationery.topZoneMm())
                || stationery.topZoneMm() < 0
                || !Double.isFinite(stationery.bottomZoneMm())
                || stationery.bottomZoneMm() < 0) {
            throw new InvalidLetterException(
                    "theme.stationery", "Reserved heights must be finite and nonnegative");
        }
        Set<String> local = new HashSet<>();
        for (FixedElement element : document.fixedElements()) {
            if (element != null) {
                local.add(element.id());
            }
        }
        var elements = new ArrayList<FixedElement>(document.fixedElements());
        Set<String> inherited = new HashSet<>();
        for (int index = 0; index < stationery.elements().size(); index++) {
            var element = stationery.elements().get(index);
            if (element == null || element.id() == null || !inherited.add(element.id())) {
                throw new InvalidLetterException(
                        "theme.stationery.elements[" + index + "]",
                        "Regions require unique nonnull identifiers");
            }
            if (!local.contains(element.id())) {
                elements.add(element);
            }
        }
        PageSpec page = document.page();
        PageSpec merged =
                new PageSpec(
                        page.size(),
                        page.marginsMm(),
                        Math.max(page.topZoneMm(), stationery.topZoneMm()),
                        Math.max(page.bottomZoneMm(), stationery.bottomZoneMm()),
                        page.signatureZoneMm(),
                        page.dimensions(),
                        page.orientation());
        return new LetterDocument(
                document.schemaVersion(),
                merged,
                document.defaultStyle(),
                document.sections(),
                elements,
                document.signature(),
                document.watermark(),
                document.theme(),
                document.components(),
                document.settings(),
                document.attachments());
    }
}
