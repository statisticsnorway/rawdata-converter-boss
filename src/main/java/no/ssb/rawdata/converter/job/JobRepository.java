package no.ssb.rawdata.converter.job;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;

import javax.inject.Singleton;
import java.util.Map;

@Singleton
@Slf4j
public class JobRepository {

    @Property(name = "vertx.pg.client")
    private Map<String, String> dbProperties;

    @EventListener
    public void onStartup(StartupEvent event) {
        Flyway.configure()
                .dataSource(
                        "jdbc:postgresql://" + dbProperties.get("host") + ":" + dbProperties.get("port") + "/" + dbProperties.get("database"),
                        dbProperties.get("user"),
                        dbProperties.get("password")
                )
                .load()
                .migrate();
    }
}
