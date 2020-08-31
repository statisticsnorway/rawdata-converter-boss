package no.ssb.rawdata.converter.job;

import de.huxhorn.sulky.ulid.ULID;
import lombok.Data;

import java.util.List;
import java.util.Objects;

@Data
public class Job {
    private String id;
    private Status status;
    private String source;
    private Document document;

    public Job() {
    }

    public void setId(String id) {
        try {
            ULID.parseULID(id);
            this.id = id;
        } catch (Exception e) {
            throw new IllegalArgumentException("Expected valid ulid, got: " + id, e);
        }
    }

    public static Job create(String id, Status status, String source, Document document) {
        Job job = new Job();
        job.setId(id);
        job.setStatus(status);
        job.setSource(source);
        job.setDocument(document);
        return job;
    }

    enum Status {
        AVAILABLE,
        ACTIVE,
        DONE;

        static Status get(String s) {
            return Status.valueOf(Objects.requireNonNull(s).toUpperCase());
        }
    }

    @Data
    static class PseudoFuncRule {
        private String name;
        private String pattern;
        private String func;

        PseudoFuncRule() {
        }
    }

    @Data
    static class PseudoConfig {
        private boolean debug = false;
        private List<PseudoFuncRule> rules;

        PseudoConfig() {
        }
    }

    @Data
    static class Document {
        private String storageRoot;
        private String storagePath;
        private long storageVersion;
        private String topic;
        private String initialPosition;
        private PseudoConfig pseudoConfig;

        Document() {
        }
    }
}
