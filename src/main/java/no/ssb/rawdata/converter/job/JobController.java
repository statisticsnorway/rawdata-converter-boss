package no.ssb.rawdata.converter.job;

import de.huxhorn.sulky.ulid.ULID;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Head;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class JobController {

    private final JobRepository repository;

    public JobController(JobRepository repository) {
        this.repository = repository;
    }

    @Head("/job/active/{source}/{id}")
    public HttpResponse<Void> isJobActive(@PathVariable String source, @PathVariable String id) {
        if (repository.readJob(id, Job.Status.ACTIVE, source) == null) {
            return HttpResponse.notFound();
        }
        return HttpResponse.ok();
    }

    @Get("/job/available/{source}")
    public HttpResponse<Job> findAvailableJob(@PathVariable String source) {
        Job job = repository.findAvailableJob(source);
        if (job == null) {
            return HttpResponse.notFound();
        }
        return HttpResponse.ok(job);
    }

    @Get("/job/available")
    public HttpResponse<Job> findAvailableJob() {
        Job job = repository.findAvailableJob();
        if (job == null) {
            return HttpResponse.notFound();
        }
        return HttpResponse.ok(job);
    }

    @Post("/job/available/{source}/{id}")
    public HttpResponse<Job> createJob(@Body Job.Document document, @PathVariable String source, @PathVariable String id) {
        try {
            ULID.parseULID(id);
        } catch (Exception e) {
            log.warn("Got invalid id: '%s' expected an ulid string".formatted(id));
            return HttpResponse.badRequest();
        }
        if (repository.createJob(id, source, document) < 1) {
            return HttpResponse.status(HttpStatus.CONFLICT); //a job with that id already exists
        }
        return HttpResponse.created(Job.create(id, Job.Status.AVAILABLE, source, document));
    }

    @Post("/job/available/{source}")
    public HttpResponse<Job> createJob(@Body Job.Document document, @PathVariable String source) {
        String id = new ULID().nextULID();
        repository.createJob(id, source, document);
        return HttpResponse.created(Job.create(id, Job.Status.AVAILABLE, source, document));
    }

    @Post("/job/done/{source}/{id}")
    public HttpResponse<Job> notifyJobDone(@PathVariable String source, @PathVariable String id) {
        if (repository.jobDone(id, source) < 1) {
            return HttpResponse.notFound();
        }
        return HttpResponse.ok();
    }
}
