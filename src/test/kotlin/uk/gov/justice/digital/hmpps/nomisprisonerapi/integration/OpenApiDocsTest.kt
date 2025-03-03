package uk.gov.justice.digital.hmpps.nomisprisonerapi.integration

import io.swagger.v3.parser.OpenAPIV3Parser
import net.minidev.json.JSONArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient(timeout = "PT60S")
class OpenApiDocsTest : IntegrationTestBase() {
  @LocalServerPort
  private var port: Int = 0

  @Test
  fun `open api docs are available`() {
    webTestClient.get()
      .uri("/swagger-ui/index.html?configUrl=/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `open api docs redirect to correct page`() {
    webTestClient.get()
      .uri("/swagger-ui.html")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().is3xxRedirection
      .expectHeader().value("Location") { it.contains("/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config") }
  }

  @Test
  fun `the open api json contains documentation`() {
    webTestClient.get()
      .uri("/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("paths").isNotEmpty
  }

  @Test
  fun `the open api json is valid and contains documentation`() {
    val result = OpenAPIV3Parser().readLocation("http://localhost:$port/v3/api-docs", null, null)
    assertThat(result.messages).isEmpty()
    assertThat(result.openAPI.paths).isNotEmpty
  }

  @Test
  fun `the open api json contains the version number`() {
    webTestClient.get()
      .uri("/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("info.version").isEqualTo(DateTimeFormatter.ISO_DATE.format(LocalDate.now()))
  }

  @Test
  fun `the generated open api for date times hasn't got the time zone`() {
    webTestClient.get()
      .uri("/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.components.schemas.ProfileDetailsResponse.properties.modifiedDateTime.example").isEqualTo("2021-07-16T12:34:56")
      .jsonPath("$.components.schemas.ProfileDetailsResponse.properties.modifiedDateTime.description")
      .isEqualTo("The time the profile info was last changed")
      .jsonPath("$.components.schemas.ProfileDetailsResponse.properties.modifiedDateTime.type").isEqualTo("string")
      .jsonPath("$.components.schemas.ProfileDetailsResponse.properties.modifiedDateTime.format").isEqualTo("date-time")
  }

  @Test
  fun `the security scheme is setup for bearer tokens`() {
    val bearerJwts = JSONArray()
    bearerJwts.addAll(listOf("read", "write"))
    webTestClient.get()
      .uri("/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.components.securitySchemes.bearer-jwt.type").isEqualTo("http")
      .jsonPath("$.components.securitySchemes.bearer-jwt.scheme").isEqualTo("bearer")
      .jsonPath("$.components.securitySchemes.bearer-jwt.bearerFormat").isEqualTo("JWT")
      .jsonPath("$.security[0].bearer-jwt")
      .isEqualTo(bearerJwts)
  }

  @Test
  fun `the open api json doesn't include LocalTime and instead uses partial-time format`() {
    webTestClient.get()
      .uri("/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("components.schemas.LocalTime").doesNotExist()
      .jsonPath("$.components.schemas.UpsertAttendanceRequest.properties.startTime.format").isEqualTo("partial-time")
  }
}
