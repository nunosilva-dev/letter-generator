package com.nsdev.lettergenerator.internal.builder;

import com.nsdev.lettergenerator.internal.content.PlainText;
import com.nsdev.lettergenerator.internal.util.ArgumentChecks;
import com.nsdev.lettergenerator.letter.AddressLayout;
import com.nsdev.lettergenerator.letter.LetterElement;
import com.nsdev.lettergenerator.letter.Party;
import com.nsdev.lettergenerator.localization.LocaleSettings;
import com.nsdev.lettergenerator.localization.ValueFormats;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.CanvasBlock;
import com.nsdev.lettergenerator.model.block.LetterPartBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.PositionedContent;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Deferred semantic content assembled in conventional letter order at final build time. */
public final class LetterParts {
    /** Captured semantic role content; calls replace the previous role. */
    private final Map<LetterElement, LetterPartBlock> parts = new EnumMap<>(LetterElement.class);

    /** Optional date formatted using final builder settings. */
    private LocalDate date;

    /** Optional local date style. */
    private Style dateStyle;

    /** Optional positioned address arrangement. */
    private AddressLayout addressLayout;

    /** Creates an empty per-builder collection. */
    public LetterParts() {}

    /**
     * Replaces one semantic part with an owned immutable content list.
     *
     * @param element non-null role
     * @param blocks ordered content
     * @param style optional local overrides
     */
    public void part(LetterElement element, List<Block> blocks, Style style) {
        ArgumentChecks.required(element, "letter element");
        List<Block> owned = List.copyOf(ArgumentChecks.required(blocks, "letter part blocks"));
        ArgumentChecks.require(!owned.isEmpty(), "Letter part must contain content");
        if (element == LetterElement.DATE) {
            date = null;
            dateStyle = null;
        }
        parts.put(element, new LetterPartBlock(element, owned, style));
    }

    /**
     * Stores a date for final locale formatting.
     *
     * @param value non-null date
     * @param style optional local style
     */
    public void date(LocalDate value, Style style) {
        date = ArgumentChecks.required(value, "letter date");
        dateStyle = style;
        parts.remove(LetterElement.DATE);
    }

    /**
     * Sets optional body-relative sender/recipient/date bounds.
     *
     * @param value non-null editable preset
     */
    public void addressLayout(AddressLayout value) {
        addressLayout = ArgumentChecks.required(value, "address layout");
        ArgumentChecks.positive(value.heightMm(), "address layout height");
    }

    /**
     * Converts a validated party to literal display lines.
     *
     * @param element SENDER or RECIPIENT
     * @param party non-null party
     */
    public void party(LetterElement element, Party party) {
        ArgumentChecks.required(party, "party");
        ArgumentChecks.require(
                party.name() != null || party.organization() != null,
                "Party needs a name or organization");
        var lines = new ArrayList<String>();
        if (party.name() != null) {
            lines.add(nonblank(party.name(), "party name"));
        }
        if (party.organization() != null) {
            lines.add(nonblank(party.organization(), "party organization"));
        }
        if (party.address() != null) {
            List<String> address =
                    ArgumentChecks.required(party.address().lines(), "address lines");
            ArgumentChecks.require(!address.isEmpty(), "Address must contain at least one line");
            address.forEach(line -> lines.add(nonblank(line, "address line")));
        }
        part(element, PlainText.region(null, String.join("\n", lines)), null);
    }

    /**
     * Stores one literal semantic phrase.
     *
     * @param element role
     * @param value nonblank literal text
     */
    public void text(LetterElement element, String value) {
        part(
                element,
                PlainText.region(
                        null,
                        nonblank(
                                value,
                                "letter " + element.name().toLowerCase(java.util.Locale.ROOT))),
                null);
    }

    /**
     * Stores a reference with a bold literal label and ordinary value.
     *
     * @param label nonblank label
     * @param value nonblank value
     */
    public void reference(String label, String value) {
        var runs =
                List.of(
                        new TextRun(
                                nonblank(label, "reference label") + ": ",
                                Style.builder().bold(true).build(),
                                null),
                        new TextRun(nonblank(value, "reference value"), null, null));
        part(LetterElement.REFERENCE, List.of(new Paragraph(runs, null)), null);
    }

    /**
     * Produces ordered parts around the body, optionally placing address fields in a canvas.
     *
     * @param body already-resolved ordinary body content
     * @param widthMm available body width
     * @param locale final formatting settings
     * @return immutable complete section content
     */
    public List<Block> assemble(List<Block> body, double widthMm, LocaleSettings locale) {
        var resolved = new EnumMap<LetterElement, LetterPartBlock>(parts);
        if (date != null) {
            resolved.put(
                    LetterElement.DATE,
                    new LetterPartBlock(
                            LetterElement.DATE,
                            PlainText.region(
                                    null, ValueFormats.format(date, "date", null, locale, null)),
                            dateStyle));
        }
        var output = new ArrayList<Block>();
        if (addressLayout != null) {
            var positioned = new ArrayList<PositionedContent>();
            positioned(resolved, positioned, LetterElement.SENDER, addressLayout.sender());
            positioned(resolved, positioned, LetterElement.DATE, addressLayout.date());
            positioned(resolved, positioned, LetterElement.RECIPIENT, addressLayout.recipient());
            if (!positioned.isEmpty()) {
                output.add(new CanvasBlock(widthMm, addressLayout.heightMm(), positioned, null));
            }
        }
        for (LetterElement element : LetterElement.values()) {
            if (element != LetterElement.CLOSING && resolved.containsKey(element)) {
                output.add(resolved.get(element));
            }
        }
        output.addAll(body);
        if (resolved.containsKey(LetterElement.CLOSING)) {
            output.add(resolved.get(LetterElement.CLOSING));
        }
        return List.copyOf(output);
    }

    /**
     * Moves one configured semantic field into a stable-ID positioned element.
     *
     * @param resolved mutable per-build role map
     * @param output per-build positioned elements
     * @param element semantic role
     * @param bounds optional configured position
     */
    private static void positioned(
            Map<LetterElement, LetterPartBlock> resolved,
            List<PositionedContent> output,
            LetterElement element,
            Bounds bounds) {
        if (bounds != null && resolved.containsKey(element)) {
            output.add(
                    new PositionedContent(
                            "letter." + element.name().toLowerCase(java.util.Locale.ROOT),
                            bounds,
                            List.of(resolved.remove(element)),
                            null));
        }
    }

    /**
     * Rejects blank literal authoring fields.
     *
     * @param value input text
     * @param name field label
     * @return unchanged validated literal text
     */
    private static String nonblank(String value, String name) {
        ArgumentChecks.require(value != null && !value.isBlank(), name + " must not be blank");
        return value;
    }
}
