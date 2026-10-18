package com.nsdev.lettergenerator.internal.render.fop;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.fop.events.Event;
import org.apache.fop.events.EventFormatter;
import org.apache.fop.events.EventListener;
import org.apache.fop.events.model.EventSeverity;

/**
 * Collects rejected FOP events for one attempt. Event IDs/severity identify failures; formatted
 * messages are retained only for diagnostics and never parsed to decide recovery.
 */
final class FopEventCollector implements EventListener {
    /** Exact event-ID suffix required before an overflow can be marked body-recoverable. */
    private static final String REGION_OVERFLOW_EVENT = ".regionOverflow";

    /** Typed FOP element identity distinguishing body overflow from fixed-region overflow. */
    private static final String BODY_REGION_ELEMENT = "fo:region-body";

    /** Event parameter carrying the overflowing region's element identity. */
    private static final String ELEMENT_NAME_PARAMETER = "elementName";

    /** Lowercase event-ID fragment identifying rejected overflow events of any severity. */
    private static final String OVERFLOW_EVENT_TOKEN = "overflow";

    /** Lowercase event-ID fragment identifying text that cannot be painted by the selected face. */
    private static final String MISSING_GLYPH_EVENT_TOKEN = "glyphnotavailable";

    /** Lowercase event-ID fragment identifying replacement of the requested font. */
    private static final String FONT_SUBSTITUTION_EVENT_TOKEN = "fontsubstitut";

    /** Ordered rejected events accumulated for this rendering attempt. */
    private final List<RenderProblem> problems = new ArrayList<>();

    /**
     * Records errors, overflow, missing-glyph and font-substitution events. Only a typed
     * region-overflow event for the body is marked recoverable.
     *
     * @param event FOP event classified by identifier, severity and typed region parameters
     */
    @Override
    public void processEvent(Event event) {
        String identifier = event.getEventID();
        String normalized = identifier.toLowerCase(Locale.ROOT);
        boolean fatal =
                event.getSeverity() == EventSeverity.ERROR
                        || event.getSeverity() == EventSeverity.FATAL;
        boolean rejectedLayout =
                normalized.contains(OVERFLOW_EVENT_TOKEN)
                        || normalized.contains(MISSING_GLYPH_EVENT_TOKEN)
                        || normalized.contains(FONT_SUBSTITUTION_EVENT_TOKEN);
        if (fatal || rejectedLayout) {
            boolean bodyOverflow =
                    identifier.endsWith(REGION_OVERFLOW_EVENT)
                            && BODY_REGION_ELEMENT.equals(event.getParam(ELEMENT_NAME_PARAMETER));
            problems.add(new RenderProblem(EventFormatter.format(event), bodyOverflow));
        }
    }

    /**
     * Returns an immutable snapshot so the renderer can combine engine events with painted-bound
     * diagnostics.
     *
     * @return immutable diagnostic snapshot in event encounter order
     */
    List<RenderProblem> problems() {
        return List.copyOf(problems);
    }
}
