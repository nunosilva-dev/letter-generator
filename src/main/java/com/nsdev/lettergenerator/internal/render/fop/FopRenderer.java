package com.nsdev.lettergenerator.internal.render.fop;

import com.nsdev.lettergenerator.exception.PdfGenerationException;
import com.nsdev.lettergenerator.internal.font.FontRegistry;
import com.nsdev.lettergenerator.internal.render.fo.FoComposer;
import com.nsdev.lettergenerator.internal.xml.SecureFoTransformer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.xml.sax.SAXException;

/** Per-call FOP state keeps the reusable generator free of shared rendering caches. */
public final class FopRenderer {
    public static final String PRODUCER = "Letter Generator";

    private FopRenderer() {}

    public static FopRenderResult render(FoComposer.Composition composition, FontRegistry fonts) {
        try {
            FopFactory factory = FopConfiguration.factory(composition.images(), fonts);
            FOUserAgent agent = factory.newFOUserAgent();
            agent.setProducer(PRODUCER);
            CheckedRenderer renderer = new CheckedRenderer(agent);
            renderer.setDocumentHandler(
                    factory.getRendererFactory()
                            .createDocumentHandler(agent, MimeConstants.MIME_PDF));
            agent.setRendererOverride(renderer);
            FopEventCollector events = new FopEventCollector();
            agent.getEventBroadcaster().addEventListener(events);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Fop fop = factory.newFop(MimeConstants.MIME_PDF, agent, output);
            SecureFoTransformer.transform(composition.xml(), fop.getDefaultHandler());
            List<RenderProblem> problems = new ArrayList<>(events.problems());
            problems.addAll(renderer.problems());
            return new FopRenderResult(output.toByteArray(), renderer.masters(), problems);
        } catch (PdfGenerationException exception) {
            throw exception;
        } catch (IOException
                | SAXException
                | ParserConfigurationException
                | TransformerException exception) {
            throw new PdfGenerationException("Unable to compose letter PDF", exception);
        } catch (RuntimeException exception) {
            // FOP also wraps checked image/font/renderer failures in unchecked exceptions.
            throw new PdfGenerationException("FOP rendering failed", exception);
        }
    }
}
