package no.ssb.rawdata.converter.job;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class JobController {

    private final JobRepository repository;

    public JobController(JobRepository repository) {
        this.repository = repository;
    }

    public HttpResponse<String> postJob(@Body JobRequest request) {
        return HttpResponse.accepted();
    }

    static class JobRequest {
        Job job;

        public JobRequest() {
        }

        public void setJob(Job job) {
            this.job = job;
        }
    }

    static class PseudoConfig {

    }

    static class Job {
        String storageRoot;
        String storagePath;
        long storageVersion;
        String topic;
        String initialPosition;
        PseudoConfig pseudoConfig;

        public Job() {
        }

        public void setStorageRoot(String storageRoot) {
            this.storageRoot = storageRoot;
        }

        public void setStoragePath(String storagePath) {
            this.storagePath = storagePath;
        }

        public void setStorageVersion(long storageVersion) {
            this.storageVersion = storageVersion;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public void setInitialPosition(String initialPosition) {
            this.initialPosition = initialPosition;
        }
    }
}
