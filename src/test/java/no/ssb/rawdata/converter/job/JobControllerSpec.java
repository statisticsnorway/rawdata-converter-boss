package no.ssb.rawdata.converter.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.huxhorn.sulky.ulid.ULID;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class JobControllerSpec {

    @Inject
    private EmbeddedServer server;

    @Inject
    private JobRepository repository;

    private final HttpClient httpClient = HttpClient.newBuilder().build();

    @BeforeEach
    void clearJobRepository() {
        repository.deleteAllJobs();
    }

    @Test
    void thatCanCheckOnActiveJob() throws IOException, InterruptedException {
        //Create a job
        repository.createJob(new ULID().nextULID(), "unknown", fromJson("""
                {
                    "storageRoot": "gs://vertical-cylinder-with-open-top-and-flat-bottom",
                    "topic": "colors",
                    "initialPosition": "FIRST"
                }
                """, Job.Document.class));

        //Take job
        Job activeJob = fromJson(httpClient.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d/job/available/unknown".formatted(server.getPort())))
                .GET()
                .timeout(Duration.of(10, SECONDS))
                .build(), HttpResponse.BodyHandlers.ofString())
                .body(), Job.class);

        //HEAD /job/active/unknown/{id} should return 200 OK
        assertThat(httpClient.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d/job/active/unknown/%s".formatted(server.getPort(), activeJob.getId())))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.of(10, SECONDS))
                .build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);

        //Mark job as done
        httpClient.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d/job/done/unknown/%s".formatted(server.getPort(), activeJob.getId())))
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.of(10, SECONDS))
                .build(), HttpResponse.BodyHandlers.ofString());

        //HEAD /job/active/unknown/{id} should now return 404 Not Found
        assertThat(httpClient.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d/job/active/unknown/%s".formatted(server.getPort(), activeJob.getId())))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.of(10, SECONDS))
                .build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(404);
    }

    @Test
    void thatCanGetAvailableJob() throws IOException, InterruptedException {
        //Create two jobs
        repository.createJob(new ULID().nextULID(), "sirius", fromJson("""
                {
                    "storageVersion": 789,
                    "topic": "cipot",
                    "initialPosition": "N/A"
                }
                """, Job.Document.class));
        repository.createJob(new ULID().nextULID(), "sirius", fromJson("""
                {
                    "storageVersion": 456,
                    "topic": "owlsinthemoss",
                    "initialPosition": "N/A"
                }
                """, Job.Document.class));

        //Query for an available job two times
        String firstResponse = httpClient.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d/job/available".formatted(server.getPort())))
                .GET()
                .timeout(Duration.of(10, SECONDS))
                .build(), HttpResponse.BodyHandlers.ofString())
                .body();
        String secondResponse = httpClient.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d/job/available".formatted(server.getPort())))
                .GET()
                .timeout(Duration.of(10, SECONDS))
                .build(), HttpResponse.BodyHandlers.ofString())
                .body();

        //Should get two jobs back in the same order they were created and with status ACTIVE
        assertThat(fromJson(firstResponse, Job.class)).isEqualToIgnoringGivenFields(fromJson("""
                {
                  "status": "ACTIVE",
                  "source": "sirius",
                  "document": {
                    "storageVersion": 789,
                    "topic": "cipot",
                    "initialPosition": "N/A"
                  }
                }
                """, Job.class), "id");
        assertThat(fromJson(secondResponse, Job.class)).isEqualToIgnoringGivenFields(fromJson("""
                {
                  "status": "ACTIVE",
                  "source": "sirius",
                  "document": {
                    "storageVersion": 456,
                    "topic": "owlsinthemoss",
                    "initialPosition": "N/A"
                  }
                }
                """, Job.class), "id");

        //Should receive 404 when there are no available jobs
        assertThat(httpClient.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d/job/available/sirius".formatted(server.getPort())))
                .GET()
                .timeout(Duration.of(10, SECONDS))
                .build(), HttpResponse.BodyHandlers.ofString())
                .statusCode()).isEqualTo(404);
    }

    @Test
    void thatCantSubmitTwoJobsWithSameId() throws IOException, InterruptedException {
        HttpRequest first = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d/job/available/altinn3/01EGP23ATM1D9B6CGC84APEA1Q".formatted(server.getPort())))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(
                        """
                                {
                                  "storageVersion": 88,
                                  "topic": "whatever",
                                  "initialPosition": "FIRST"
                                }
                                """
                ))
                .timeout(Duration.of(10, SECONDS))
                .build();

        httpClient.send(first, HttpResponse.BodyHandlers.ofString());

        HttpRequest second = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d/job/available/altinn3/01EGP23ATM1D9B6CGC84APEA1Q".formatted(server.getPort())))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(
                        """
                                {
                                  "storageVersion": 44,
                                  "topic": "pffft",
                                  "initialPosition": "SECOND"
                                }
                                """
                ))
                .timeout(Duration.of(10, SECONDS))
                .build();

        HttpResponse<String> response = httpClient.send(second, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(409);

        Job want = fromJson("""
                {
                  "id": "01EGP23ATM1D9B6CGC84APEA1Q",  
                  "status": "AVAILABLE",
                  "source": "altinn3",
                  "document": {
                    "storageVersion": 88,
                    "topic": "whatever",
                    "initialPosition": "FIRST"
                  }
                }
                """, Job.class);

        List<Job> got = repository.readAllJobs();
        assertThat(got).hasSize(1);
        assertThat(got.get(0)).isEqualTo(want);
    }

    @Test
    void thatSubmitJobWorks() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d/job/available/freg".formatted(server.getPort())))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(
                        """
                                {
                                  "storageRoot": "gs://bucket",
                                  "storagePath": "/tmp",
                                  "storageVersion": 42,
                                  "topic": "data",
                                  "initialPosition": "LAST",
                                  "pseudoConfig": {
                                    "debug": true,
                                    "rules": [
                                      {
                                        "name": "fodselsnummer",
                                        "pattern": "**/fodselsnummer",
                                        "func": "fpe-fnr(secret1)"
                                      }
                                    ]
                                  }
                                }
                                """
                ))
                .timeout(Duration.of(10, SECONDS))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        Job jobCreated = new ObjectMapper().readValue(response.body(), Job.class);

        Job want = fromJson("""
                {
                  "status": "AVAILABLE",
                  "source": "freg",
                  "document": {
                    "storageRoot": "gs://bucket",
                    "storagePath": "/tmp",
                    "storageVersion": 42,
                    "topic": "data",
                    "initialPosition": "LAST",
                    "pseudoConfig": {
                      "debug": true,
                      "rules": [
                        {
                          "name": "fodselsnummer",
                          "pattern": "**/fodselsnummer",
                          "func": "fpe-fnr(secret1)"
                        }
                      ]
                    }
                  }
                }
                """, Job.class);

        assertThat(jobCreated).isEqualToIgnoringGivenFields(want, "id");
        Job jobStored = repository.readJob(jobCreated.getId(), jobCreated.getStatus(), jobCreated.getSource());
        assertThat(jobStored).isEqualTo(jobCreated);
    }

    private static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return new ObjectMapper().readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
