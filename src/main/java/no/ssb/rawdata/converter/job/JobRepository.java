package no.ssb.rawdata.converter.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
import java.util.ArrayList;
import java.util.List;

@Singleton
@Slf4j
public class JobRepository {

    @Inject
    @Named("default")
    private final DataSource dataSource;

    public JobRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Job readJob(String id, Job.Status status, String source) {
        try (Connection con = dataSource.getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM job WHERE id=? AND status=? AND source=?");
            ps.setString(1, id);
            ps.setString(2, status.toString());
            ps.setString(3, source);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }
            return Job.create(
                    rs.getString("id"),
                    Job.Status.get(rs.getString("status")),
                    rs.getString("source"),
                    new ObjectMapper().readValue(rs.getString("document"), Job.Document.class)
            );
        } catch (Exception e) {
            throw new RuntimeException("Could not read job, id: %s".formatted(id), e);
        }
    }

    public Job findAvailableJob() {
        try (Connection con = dataSource.getConnection()) {
            String sql = """
                    UPDATE job
                    SET status = 'ACTIVE'
                    WHERE id =
                        (SELECT id FROM job WHERE status = 'AVAILABLE' ORDER BY id LIMIT 1)
                    RETURNING *                    
                    """;
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (!rs.next()) {
                return null;
            }
            return Job.create(
                    rs.getString("id"),
                    Job.Status.get(rs.getString("status")),
                    rs.getString("source"),
                    new ObjectMapper().readValue(rs.getString("document"), Job.Document.class)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failure when trying to find available job", e);
        }
    }

    public Job findAvailableJob(String source) {
        try (Connection con = dataSource.getConnection()) {
            String sql = """
                    UPDATE job
                    SET status = 'ACTIVE'
                    WHERE id =
                        (SELECT id FROM job WHERE status = 'AVAILABLE' AND source = ? ORDER BY id LIMIT 1)
                    RETURNING *                    
                    """;
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, source);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }
            return Job.create(
                    rs.getString("id"),
                    Job.Status.get(rs.getString("status")),
                    rs.getString("source"),
                    new ObjectMapper().readValue(rs.getString("document"), Job.Document.class)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failure when trying to find available job, source: %s".formatted(source), e);
        }
    }

    public int createJob(String id, String source, Job.Document document) {
        PGobject pgObject;
        try {
            pgObject = new PGobject();
            pgObject.setType("jsonb");
            pgObject.setValue(new ObjectMapper().writeValueAsString(document));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create postgres json object from job: %s".formatted(document), e);
        }
        try (Connection con = dataSource.getConnection()) {
            PreparedStatement ps = con.prepareStatement("INSERT INTO job (id,status,source,document) VALUES (?,?,?,?) ON CONFLICT (id) DO NOTHING");
            ps.setString(1, id);
            ps.setString(2, Job.Status.AVAILABLE.toString());
            ps.setString(3, source);
            ps.setObject(4, pgObject);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create job, id: %s, source: %s, document: %s".formatted(id, source, document), e);
        }
    }

    public int jobDone(String id, String source) {
        try (Connection con = dataSource.getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE job SET status = 'DONE' WHERE id = ? AND source = ?");
            ps.setString(1, id);
            ps.setString(2, source);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to mark job as done, id: %s, source: %s".formatted(id, source), e);
        }
    }

    List<Job> readAllJobs() {
        try (Connection con = dataSource.getConnection()) {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM job ORDER BY id");
            List<Job> jobs = new ArrayList<>();
            while (rs.next()) {
                jobs.add(
                        Job.create(
                                rs.getString("id"),
                                Job.Status.get(rs.getString("status")),
                                rs.getString("source"),
                                new ObjectMapper().readValue(rs.getString("document"), Job.Document.class)
                        )
                );
            }
            return jobs;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read jobs", e);
        }
    }

    int deleteAllJobs() {
        try (Connection con = dataSource.getConnection()) {
            Statement stmt = con.createStatement();
            return stmt.executeUpdate("TRUNCATE TABLE job");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete jobs", e);
        }
    }
}
