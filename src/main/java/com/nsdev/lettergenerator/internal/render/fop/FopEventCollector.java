package com.nsdev.lettergenerator.internal.render.fop;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.fop.events.Event;
import org.apache.fop.events.EventFormatter;
import org.apache.fop.events.EventListener;
import org.apache.fop.events.model.EventSeverity;

final class FopEventCollector implements EventListener {
    private static final String REGION_OVERFLOW_EVENT = ".regionOverflow";
    private static final String BODY_REGION_ELEMENT = "fo:region-body";
    private static final String ELEMENT_NAME_PARAMETER = "elementName";
    private static final String OVERFLOW_EVENT_TOKEN = "overflow";
    private static final String MISSING_GLYPH_EVENT_TOKEN = "glyphnotavailable";
    private static final String FONT_SUBSTITUTION_EVENT_TOKEN = "fontsubstitut";
    private final List<RenderProblem> problems = new ArrayList<>();

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

    List<RenderProblem> problems() {
        return List.copyOf(problems);
    }
}
