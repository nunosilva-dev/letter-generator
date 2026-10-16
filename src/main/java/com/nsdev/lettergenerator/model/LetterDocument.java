package com.nsdev.lettergenerator.model;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import com.nsdev.lettergenerator.model.layout.PageSpec;
import com.nsdev.lettergenerator.model.layout.Signature;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.watermark.Watermark;
import java.util.List;

/**
 * Complete immutable generation request. Optional page and fixed-element fields select defaults;
 * content is validated at generation time.
 */
public record LetterDocument(
        int schemaVersion,
        PageSpec page,
        Style defaultStyle,
        List<Section> sections,
        List<FixedElement> fixedElements,
        Signature signature,
        Watermark watermark) {
    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    public LetterDocument {
        if (page == null) {
            page = PageSpec.defaults();
        }
        sections = ImmutableLists.copyNullable(sections);
        fixedElements =
                fixedElements == null ? List.of() : ImmutableLists.copyNullable(fixedElements);
    }
}
