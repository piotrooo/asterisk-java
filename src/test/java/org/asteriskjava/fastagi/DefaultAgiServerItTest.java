package org.asteriskjava.fastagi;

import org.asteriskjava.AsteriskVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.MinimumDurationRunningStartupCheckStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.HashMap;

import static java.time.Duration.ofSeconds;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.asteriskjava.AsteriskVersion.ASTERISK_18;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.Testcontainers.exposeHostPorts;
import static org.testcontainers.containers.BindMode.READ_ONLY;
import static org.testcontainers.utility.DockerImageName.parse;

@Testcontainers
class DefaultAgiServerItTest {
    @Container
    private static final GenericContainer<?> asterisk = new GenericContainer<>(parse("andrius/asterisk"))
        .withClasspathResourceMapping("extensions.conf", "/etc/asterisk/extensions.conf", READ_ONLY)
        .withAccessToHost(true)
        // Wait for Asterisk startup
        .withStartupCheckStrategy(new MinimumDurationRunningStartupCheckStrategy(ofSeconds(5)));

    @BeforeAll
    static void beforeAll() {
        exposeHostPorts(4573);
    }

    @Test
    void shouldName() throws Exception {
        //given
        SampleAgiScript sampleAgiScript = new SampleAgiScript();

        HashMap<String, AgiScript> mappings = new HashMap<>();
        mappings.put("sample", sampleAgiScript);
        SimpleMappingStrategy simpleMappingStrategy = new SimpleMappingStrategy();
        simpleMappingStrategy.setMappings(mappings);

        DefaultAgiServer defaultAgiServer = new DefaultAgiServer(simpleMappingStrategy);
        newSingleThreadExecutor().submit(() -> {
            try {
                defaultAgiServer.startup();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        //when
        asterisk.execInContainer("asterisk", "-rx", "channel originate Local/1300/1234 extension");

        //then
        await()
            .atMost(ofSeconds(2))
            .untilAsserted(() -> assertThat(sampleAgiScript.asteriskVersion).isEqualTo(ASTERISK_18));
    }

    static class SampleAgiScript implements AgiScript {
        private AsteriskVersion asteriskVersion;

        @Override
        public void service(AgiRequest request, AgiChannel channel) throws AgiException {
            this.asteriskVersion = channel.getAsteriskVersion();
        }
    }
}
