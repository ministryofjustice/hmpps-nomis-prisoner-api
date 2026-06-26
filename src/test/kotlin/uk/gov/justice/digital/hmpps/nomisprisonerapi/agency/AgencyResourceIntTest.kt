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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Agency
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Prison
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PrisonRepository
import java.time.LocalDate

class AgencyResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var agencyLocationRepository: AgencyLocationRepository

  @Autowired
  private lateinit var agencyRepository: AgencyRepository

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @DisplayName("GET /prison/{prisonId}")
  @Nested
  inner class GetPrison {
    lateinit var legacyGenericAgency: AgencyLocation
    lateinit var approvedPremise: Agency
    lateinit var prison: Prison

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        legacyGenericAgency = agencyLocation(
          agencyLocationId = "XXI",
          description = "HMP XXI",
          type = "INST",
        )
        prison = prison(
          agencyLocationId = "AAI",
          description = "HMP AAI",
          districtCode = "LONDON",
        )
        approvedPremise = agency(
          agencyLocationId = "THA029",
          description = "Approved Premises",
          districtCode = "40",
          active = false,
          type = "APPR",
          deactivationDate = LocalDate.parse("2022-01-01"),
          updateAllowed = false,
        )
      }
    }

    @AfterEach
    fun tearDown() {
      agencyLocationRepository.deleteById(legacyGenericAgency.id)
      agencyRepository.delete(approvedPremise)
      prisonRepository.delete(prison)
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
        val prison: PrisonResponse = webTestClient.get().uri("/prison/AAI")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(prison.prisonId).isEqualTo("AAI")
        assertThat(prison.description).isEqualTo("HMP AAI")
        assertThat(prison.district?.description).isEqualTo("London")
        assertThat(prison.active).isTrue
        assertThat(prison.deactivationDate).isNull()
        assertThat(prison.updateAllowed).isTrue
      }

      @Test
      fun `will return generic agency location details for a prison`() {
        val agency: AgencyResponse = webTestClient.get().uri("/agency-location/AAI")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(agency.agencyId).isEqualTo("AAI")
        assertThat(agency.description).isEqualTo("HMP AAI")
        assertThat(agency.active).isTrue
        assertThat(agency.deactivationDate).isNull()
        assertThat(agency.updateAllowed).isTrue
      }

      @Test
      fun `will return generic agency details for an approved premises`() {
        val agency: AgencyResponse = webTestClient.get().uri("/agency-location/THA029")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(agency.agencyId).isEqualTo("THA029")
        assertThat(agency.description).isEqualTo("Approved Premises")
        assertThat(agency.type.description).isEqualTo("Approved Premises")
        assertThat(agency.active).isFalse
        assertThat(agency.deactivationDate).isEqualTo(LocalDate.parse("2022-01-01"))
        assertThat(agency.updateAllowed).isFalse
      }

      @Test
      fun `will return details for an approved premises`() {
        val agency: AgencyResponse = webTestClient.get().uri("/agency/THA029")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(agency.agencyId).isEqualTo("THA029")
        assertThat(agency.description).isEqualTo("Approved Premises")
        assertThat(agency.district?.description).isEqualTo("Thames Valley")
        assertThat(agency.type.description).isEqualTo("Approved Premises")
        assertThat(agency.active).isFalse
        assertThat(agency.deactivationDate).isEqualTo(LocalDate.parse("2022-01-01"))
        assertThat(agency.updateAllowed).isFalse
      }

      @Test
      fun `will not find as agency when is prison`() {
        webTestClient.get().uri("/agency/AAI")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will not find as prison when is agency`() {
        webTestClient.get().uri("/prison/THA029")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }
}
