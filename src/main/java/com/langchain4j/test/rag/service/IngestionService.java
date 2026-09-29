package com.langchain4j.test.rag.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import dev.langchain4j.data.document.Metadata;
import java.util.regex.Pattern;


@Component
public class IngestionService {

    private final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private static final int MAX_CHARS = 1500;
    private static final int LAST_ARTICLE = 308;

    private static final Pattern PART     = Pattern.compile("^Part-\\s*(\\d+)$");
    private static final Pattern SCHEDULE = Pattern.compile("^Schedule-\\s*(\\d+)$");
    private static final Pattern ARTICLE  = Pattern.compile("^(\\d{1,3})\\.\\s+\\S.*");
    private static final Pattern PAGE_NO  = Pattern.compile("^\\d{1,3}$");
    private static final Pattern CLAUSE   = Pattern.compile("(?m)^(?=\\(\\d+\\)\\s)");

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public IngestionService(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    private static class Unit {
        final String kind, number, part, partTitle;
        final StringBuilder text = new StringBuilder();
        private Unit(String kind, String number, String part, String partTitle) {
            this.kind = kind;
            this.number = number;
            this.part = part;
            this.partTitle = partTitle;
        }
    }

    public void ingestDocument() throws IOException {
        List<TextSegment> segments = parse(Path.of(IngestionService.class.getResource("/docs/Constitution-of-Nepal.pdf").getFile()));
        log.info("Parsed {} segments", segments.size());

        embeddingStore.removeAll();                       // avoid duplicates on re-run
        for (int i = 0; i < segments.size(); i += 32) {   // batch to keep Ollama happy
            List<TextSegment> batch = segments.subList(i, Math.min(i + 32, segments.size()));
            List<Embedding> embeddings = embeddingModel.embedAll(batch).content();
            embeddingStore.addAll(embeddings, batch);
        }
    }

    List<TextSegment> parse(Path pdf) throws IOException {
        String raw;
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            raw = stripper.getText(doc);
        }

        List<Unit> units = new ArrayList<>();
        Unit cur = null;
        String part = "", partTitle = "";
        boolean partTitleNext = false;
        int lastArticle = 0;

        for (String rawLine : raw.split("\\R")) {
            String line = rawLine.strip();
            if (line.isEmpty() || PAGE_NO.matcher(line).matches()) continue;   // blank / page numbers

            if (partTitleNext) { partTitle = line; partTitleNext = false; continue; }

            Matcher m = PART.matcher(line);
            if (m.matches()) { part = m.group(1); partTitleNext = true; continue; }

            // Schedules only count after the last article (avoids "Schedule-2" references in text)
            m = SCHEDULE.matcher(line);
            if (lastArticle >= LAST_ARTICLE && m.matches()) {
                cur = new Unit("schedule", m.group(1), "", "");
                units.add(cur);
                continue;
            }

            boolean inSchedules = cur != null && cur.kind.equals("schedule");
            m = ARTICLE.matcher(line);
            // monotonic check: only accept article N+1, so list items like "2. ..." don't split
            if (!inSchedules && m.matches() && Integer.parseInt(m.group(1)) == lastArticle + 1) {
                lastArticle++;
                cur = new Unit("article", m.group(1), part, partTitle);
                units.add(cur);
            }
            if (cur != null) cur.text.append(line).append('\n');   // TOC/preamble skipped (cur == null)
        }
        log.info("Last article parsed: {} (expected {})", lastArticle, LAST_ARTICLE);

        List<TextSegment> out = new ArrayList<>();
        units.forEach(u -> out.addAll(toSegments(u)));
        return out;
    }

    private List<TextSegment> toSegments(Unit u) {
        String text = u.text.toString().replaceAll("[ \\t]+", " ").strip();
        String title = "";
        int colon = text.indexOf(':');
        if (u.kind.equals("article") && colon > 0 && colon < 200) {
            title = text.substring(text.indexOf('.') + 1, colon).replace('\n', ' ').strip();
        }

        String header = u.kind.equals("article")
                ? "Constitution of Nepal | Part %s: %s | Article %s (%s)".formatted(u.part, u.partTitle, u.number, title)
                : "Constitution of Nepal | Schedule %s".formatted(u.number);

        List<String> pieces = text.length() <= MAX_CHARS ? List.of(text) : splitByClause(text);
        List<TextSegment> segs = new ArrayList<>();
        for (int i = 0; i < pieces.size(); i++) {
            Metadata md = new Metadata().put("type", u.kind).put(u.kind, u.number);
            if (!u.part.isEmpty()) md.put("part", u.part);
            if (!title.isBlank()) md.put("title", title);
            md.put("chunk", i);
            segs.add(TextSegment.from(header + "\n" + pieces.get(i), md));
        }
        return segs;
    }

    private List<String> splitByClause(String text) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (String p : CLAUSE.split(text)) {
            if (sb.length() > 0 && sb.length() + p.length() > MAX_CHARS) {
                out.add(sb.toString().strip());
                sb.setLength(0);
            }
            sb.append(p);
        }
        if (sb.length() > 0) out.add(sb.toString().strip());
        return out;
    }
}
