package uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class InfoTest : IntegrationTestBase() {

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Test
  fun `Info page is accessible`() {
    webTestClient.get()
      .uri("/info")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("build.name").isEqualTo("hmpps-nomis-prisoner-api")
  }

  @Test
  fun `Info page reports version`() {
    webTestClient.get().uri("/info")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("build.version").value<String> {
        assertThat(it).startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
      }
  }

  @Test
  fun `Info page reports agency feature switches`() {
    nomisDataBuilder.build {
      externalService("SOME_SERVICE") {
        serviceAgencySwitch("BXI")
        serviceAgencySwitch("MDI")
      }
      externalService("ANOTHER_SERVICE")
    }

    webTestClient.get().uri("/info")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.agencySwitches").value<List<Map<String, List<String>>>> {
        assertThat(it).containsExactlyInAnyOrder(
          mapOf("SOME_SERVICE" to listOf("BXI", "MDI")),
          mapOf("ANOTHER_SERVICE" to listOf()),
        )
      }
  }
}
