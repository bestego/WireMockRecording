package org.acme.rest.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
//import lombok.extern.slf4j.Slf4j;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;


@QuarkusTest
public class CountriesResourceTest {

    WireMockServer wireMockServer;
    WireMock wireMockClient;
    int portWM = 8089;
    boolean recording = false;

    @BeforeEach
    public void startWireMock() {

        wireMockServer = new WireMockServer(options().port(portWM)); //No-args constructor will start on port 8080, no HTTPS
        wireMockServer.start();
        configureFor(portWM);
    }

    @AfterEach
    public void stopWireMock() {
        if ( recording ) {
            recording = false;
            wireMockClient.stopStubRecording();
            wireMockClient.shutdown();
            System.out.println("wireMockClient: recording stopped .. mapping generated");
        }
        wireMockServer.stop();
    }

    @Test
    public void testCountryNameEndpoint() throws Exception {

        String restUrl = "/name/greece";
        String targetBaseUrl = "http://restcountries.eu:80/rest/v2";

        if (! WireMockHelper.isMapped(restUrl)) {
            recording = true;
            wireMockClient = new WireMock(portWM);
            wireMockClient.startStubRecording(targetBaseUrl);
            System.out.println("restUrl:"+restUrl+" not mapped .. recording started");
        } else {
            System.out.println("restUrl:"+restUrl+" already mapped");
        }

        given()
                .when()
                .baseUri("http://localhost:8081/country") // Note: 8081 seems internal port regardless application.properties! (see notification in terminal window)
                .get(restUrl)
                .then()
                .statusCode(200)
                .body("$.size()", is(1),
                        "[0].alpha2Code", is("GR"),
                        "[0].capital", is("Athens"),
                        "[0].currencies.size()", is(1),
                        "[0].currencies[0].name", is("Euro")
                );
    }

    /**
     * This second test has no added value for testing the application itself
     * Its only purpose it to have multiple tests available to verify the correct WireMock setup
     * @throws Exception
     */
    @Test
    public void testCountryNameNetherlands() throws Exception {

        String restUrl = "/name/netherlands";
        String targetBaseUrl = "http://restcountries.eu/rest/v2";

        if (! WireMockHelper.isMapped(restUrl)) {
            recording = true;
            wireMockClient = new WireMock(portWM);
            wireMockClient.startStubRecording(targetBaseUrl);
            System.out.println("restUrl:"+restUrl+" not mapped .. recording started");
        } else {
            System.out.println("restUrl:"+restUrl+" already mapped");
        }

        given()
                .when()
                .baseUri("http://localhost:8081/country") // Note: 8081 seems internal port regardless application.properties! (see notification in terminal window)
                .get(restUrl)
                .then()
                .statusCode(200)
                .body("$.size()", is(1),
                        "[0].capital", is("Amsterdam")
                );
    }
}

class WireMockHelper {

    static boolean isMapped(String restUrl) {
        Path path = Paths.get("src/test/resources/mappings");
        Stream<Path> files;
        try {
            files = Files.find(path, Integer.MAX_VALUE, (p, a) -> p.toString().toLowerCase().endsWith(".json"));
        } catch (IOException e) {
            return false;
        }
        return files.anyMatch(f -> fileContains(f, restUrl));
    }

    static boolean fileContains(Path file, String needle) {
        String regex = "[ ]*\"url\"[ ]*:[ ]*\"" + needle + "\"[ ,]*";
        try {
            return Files.lines(file)
                    .anyMatch(s -> s.matches(regex));
        } catch (IOException e) {
            return false;
        }
    }
}