package no.ssb.rawdata.converter.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.ssb.rawdata.converter.job.JobController.Job;
import org.postgresql.util.PGobject;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Singleton
@Slf4j
public class JobRepository {

    @Inject
    @Named("default")
    private final DataSource dataSource;

    public JobRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public int createJob(UUID id, Job job) {
        PGobject document;
        try {
            PGobject o = new PGobject();
            o.setType("jsonb");
            o.setValue(new ObjectMapper().writeValueAsString(job));
            document = o;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to create postgres json object from job: %s", job), e);
        }
        try (Connection con = dataSource.getConnection()) {
            PreparedStatement ps = con.prepareStatement("INSERT INTO job (id,document) VALUES (?,?) ON CONFLICT (id) DO NOTHING");
            ps.setObject(1, id);
            ps.setObject(2, document);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Failed to create job: %s", job), e);
        }
    }

    public Map<UUID, Job> readJob(UUID id) {
        Map<UUID, Job> job = new HashMap<>();
        try (Connection con = dataSource.getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM job WHERE id=?");
            ps.setObject(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                job.put((UUID) rs.getObject("id"), new ObjectMapper().readValue(rs.getString("document"), Job.class));
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to read job. id=%s", id), e);
        }
        return job;
    }

    public Map<UUID, Job> readJobs() {
        Map<UUID, Job> job = new HashMap<>();
        try (Connection con = dataSource.getConnection()) {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM job");
            while (rs.next()) {
                job.put((UUID) rs.getObject("id"), new ObjectMapper().readValue(rs.getString("document"), Job.class));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read jobs", e);
        }
        return job;
    }
}
