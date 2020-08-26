package no.ssb.rawdata.converter.job;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class JobController {

    private final JobRepository repository;

    public JobController(JobRepository repository) {
        this.repository = repository;
    }

    @Post("/job/{id}")
    public HttpResponse<JobResponse> postJob(@Body JobRequest request, @PathVariable UUID id) {
        try {
            int jobsCreated = repository.createJob(id, request.getJob());
            if (jobsCreated < 1) {
                return HttpResponse.status(HttpStatus.CONFLICT);
            }
            return HttpResponse.created(new JobResponse(id, request.getJob()));
        } catch (Exception e) {
            log.error(String.format("Failed to create job %s", request.getJob()), e);
            return HttpResponse.serverError();
        }
    }

    @Post("/job")
    public HttpResponse<JobResponse> postJob(@Body JobRequest request) {
        try {
            UUID id = UUID.randomUUID();
            repository.createJob(id, request.getJob());
            return HttpResponse.created(new JobResponse(id, request.getJob()));
        } catch (Exception e) {
            log.error(String.format("Failed to create job %s", request.getJob()), e);
            return HttpResponse.serverError();
        }
    }

    @Get("/job/{id}")
    public HttpResponse<JobResponse> getJob(@PathVariable UUID id) {
        try {
            Map<UUID, Job> jobs = repository.readJob(id);
            if (jobs.isEmpty()) {
                return HttpResponse.notFound();
            }
            return HttpResponse.ok(
                    jobs
                            .entrySet()
                            .stream()
                            .map(entry -> new JobResponse(entry.getKey(), entry.getValue()))
                            .findFirst()
                            .get()
            );
        } catch (Exception e) {
            log.error("Could not read jobs", e);
            return HttpResponse.serverError();
        }
    }

    @Get("/job")
    public HttpResponse<List<JobResponse>> getJobs() {
        try {
            return HttpResponse.ok(
                    repository
                            .readJobs()
                            .entrySet()
                            .stream()
                            .map(entry -> new JobResponse(entry.getKey(), entry.getValue()))
                            .collect(Collectors.toList())
            );
        } catch (Exception e) {
            log.error("Could not read jobs", e);
            return HttpResponse.serverError();
        }
    }

    @Data
    static class JobRequest {
        Job job;
    }

    @Data
    static class JobResponse {
        final UUID id;
        final Job job;
    }

    @Data
    static class PseudoConfig {

    }

    @Data
    static class Job {
        String storageRoot;
        String storagePath;
        long storageVersion;
        String topic;
        String initialPosition;
        PseudoConfig pseudoConfig;
        Job() {}
    }
}
