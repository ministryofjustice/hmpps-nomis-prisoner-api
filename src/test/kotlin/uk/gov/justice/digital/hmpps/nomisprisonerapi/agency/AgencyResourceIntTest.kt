package uk.gov.justice.digital.hmpps.nomisprisonerapi.agency

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository

class AgencyResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var agencyLocationRepository: AgencyLocationRepository

  @DisplayName("GET /prison/{prisonId}")
  @Nested
  inner class GetPrison {
    lateinit var agency: AgencyLocation

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        agency = agencyLocation(agencyLocationId = "XXI", description = "HMP XXI", type = "INST")
      }
    }

    @AfterEach
    fun tearDown() {
      agencyLocationRepository.deleteById(agency.id)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prison/XXI")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prison/XXI")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prison/XXI")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `will return 404 if prison does not exist`() {
        webTestClient.get().uri("/prison/ZZI")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return prison details`() {
        val prison: PrisonResponse = webTestClient.get().uri("/prison/XXI")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(prison.prisonId).isEqualTo("XXI")
        assertThat(prison.description).isEqualTo("HMP XXI")
      }

      @Test
      fun `will return generic agency location details`() {
        val agency: AgencyResponse = webTestClient.get().uri("/agency-location/XXI")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(agency.agencyId).isEqualTo("XXI")
        assertThat(agency.description).isEqualTo("HMP XXI")
      }

      @Test
      fun `will not find as non-prison agency`() {
        webTestClient.get().uri("/agency/XXI")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }
}
