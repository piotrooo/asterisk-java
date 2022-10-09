package org.asteriskjava.manager;

import org.asteriskjava.manager.action.ShowDialplanAction;
import org.asteriskjava.manager.event.ListDialplanEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.MinimumDurationRunningStartupCheckStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.BindMode.READ_ONLY;
import static org.testcontainers.utility.DockerImageName.parse;

@Testcontainers
class ManagerConnectionItTest {
    @Container
    private static final GenericContainer<?> asterisk = new GenericContainer<>(parse("andrius/asterisk"))
        .withClasspathResourceMapping("manager.conf", "/etc/asterisk/manager.conf", READ_ONLY)
        .withAccessToHost(true)
        .withExposedPorts(5038)
        // Wait for Asterisk startup
        .withStartupCheckStrategy(new MinimumDurationRunningStartupCheckStrategy(ofSeconds(5)));
    private static ManagerConnection managerConnection;

    @BeforeAll
    static void beforeAll() throws Exception {
        ManagerConnectionFactory managerConnectionFactory = new ManagerConnectionFactory(asterisk.getHost(), asterisk.getFirstMappedPort(), "piotrooo", "123qwe");
        managerConnection = managerConnectionFactory.createManagerConnection();

        managerConnection.login();
    }

    @AfterAll
    static void afterAll() {
        managerConnection.logoff();
    }

    @Test
    void shouldHandleIncludeContextAndSwitchAndIgnorePatternProperties() throws Exception {
        //given
        List<ListDialplanEvent> listDialplanEvents = new ArrayList<>();

        managerConnection.addEventListener(new AbstractManagerEventListener() {
            @Override
            public void handleEvent(ListDialplanEvent event) {
                listDialplanEvents.add(event);
            }
        });

        //when
        managerConnection.sendAction(new ShowDialplanAction());

        //then
        assertThat(listDialplanEvents)
            .extracting(ListDialplanEvent::getIncludeContext)
            .contains("demo", "iaxtel700", "parkedcalls");

        assertThat(listDialplanEvents)
            .extracting(ListDialplanEvent::getSwitch)
            .contains("DUNDi/e164", "Lua/");

        assertThat(listDialplanEvents)
            .extracting(ListDialplanEvent::getIgnorePattern)
            .contains("9");
    }
}
