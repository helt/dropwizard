package io.dropwizard.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.concurrent.Executors;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.common.io.Resources;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.dropwizard.util.Duration;

public class JerseyIgnoreRequestUserAgentHeaderFilterTest {
    private static final long DEFAULT_CONNECTION_TIMEOUT = 500L;
    private static final long DEFAULT_TIMEOUT = 1000L;

    @ClassRule
    public static DropwizardAppRule<Configuration> APP_RULE =
            new DropwizardAppRule<>(TestApplication.class, Resources.getResource("yaml/jerseyIgnoreRequestUserAgentHeaderFilterTest.yml").getPath());
    
    public final URI testUri = URI.create("http://localhost:" + APP_RULE.getLocalPort());

    private JerseyClientBuilder clientBuilder;
    
    private JerseyClientConfiguration clientConfiguration;

    @Before
    public void setup() {
        clientConfiguration = new JerseyClientConfiguration();
        clientConfiguration.setConnectionTimeout(Duration.milliseconds(DEFAULT_CONNECTION_TIMEOUT));
        clientConfiguration.setTimeout(Duration.milliseconds(DEFAULT_TIMEOUT));
        clientBuilder = new JerseyClientBuilder(new MetricRegistry())
                .using(Executors.newSingleThreadExecutor(), Jackson.newObjectMapper());
    }
    
    @Test
    public void clientIsSetRequestIsNotSet() {
        clientConfiguration.setUserAgent(Optional.of("ClientUserAgentHeaderValue"));
        assertThat(
                clientBuilder.using(clientConfiguration).
                build("ClientName").target(testUri + "/user_agent")
                        .request()
                        .get(String.class)
        ).isEqualTo("ClientUserAgentHeaderValue");
    }
    
    @Test
    public void clientIsNotSetRequestIsSet() {
        assertThat(
                clientBuilder.build("ClientName").target(testUri + "/user_agent")
                        .request().header("User-Agent", "RequestUserAgentHeaderValue")
                        .get(String.class)
        ).isEqualTo("RequestUserAgentHeaderValue");
    }

    @Test
    public void clientIsNotSetRequestIsNotSet() {
        assertThat(false);        
        assertThat(
                clientBuilder.build("ClientName").target(testUri + "/user_agent")
                        .request()
                        .get(String.class)
        ).isEqualTo("ClientName");
    }

    @Test
    public void clientIsSetRequestIsSet() {
        clientConfiguration.setUserAgent(Optional.of("ClientUserAgentHeaderValue"));
        assertThat(
                clientBuilder.build("ClientName").target(testUri + "/user_agent")
                        .request().header("User-Agent", "RequestUserAgentHeaderValue")
                        .get(String.class)
        ).isEqualTo("RequestUserAgentHeaderValue");
    }
    
    
    @Path("/")
    public static class TestResource {

       @GET
        @Path("user_agent")
        public String getReturnUserAgentHeader(@HeaderParam("User-Agent") String userAgentHeader) {
            return userAgentHeader;
        }
        
    }

    public static class TestApplication extends Application<Configuration> {
        public static void main(String[] args) throws Exception {
            new TestApplication().run(args);
        }

        @Override
        public void run(Configuration configuration, Environment environment) throws Exception {
            environment.jersey().register(TestResource.class);
        }
    }



}