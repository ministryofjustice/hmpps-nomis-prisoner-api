package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonerprofile

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonerprofile.api.PrisonerPhysicalAttributesResponse

class PrisonerProfileIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var offenderBookingRepository: OffenderBookingRepository

  @Autowired
  private lateinit var repository: Repository

  @AfterEach
  fun cleanUp() {
    repository.deleteOffenders()
  }

  @DisplayName("Temporary test to check JPA and test builders")
  @Nested
  inner class TestJpa {
    @Test
    fun `should save and load physical attributes`() {
      var booking: OffenderBooking? = null
      nomisDataBuilder.build {
        offender(nomsId = "A1111AA") {
          booking = booking {
            physicalAttributes(
              heightCentimetres = 180,
              heightFeet = 5,
              heightInches = 11,
              weightKilograms = 80,
              weightPounds = 180,
            )
            physicalAttributes(
              heightCentimetres = null,
              heightFeet = null,
              heightInches = null,
              weightKilograms = null,
              weightPounds = null,
            )
          }
        }
      }

      repository.runInTransaction {
        offenderBookingRepository.findLatestByOffenderNomsId("A1111AA").also {
          assertThat(it!!.physicalAttributes)
            .extracting("id.offenderBooking.bookingId", "id.sequence", "heightCentimetres", "heightFeet", "heightInches", "weightKilograms", "weightPounds")
            .containsExactly(
              tuple(booking!!.bookingId, 1L, 180, 5, 11, 80, 180),
              tuple(booking!!.bookingId, 2L, null, null, null, null, null),
            )
        }
      }
    }
  }

  @Nested
  @DisplayName("GET /prisoners/{offenderNo}/physical-attributes")
  inner class GetPhysicalAttributes {
    @Nested
    inner class Security {
      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/A1234AA/physical-attributes")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/A1234AA/physical-attributes")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/A1234AA/physical-attributes")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      // TODO SDIT-1817 Remove this once we implement the service and test it for real
      @Test
      fun `should call the service`() {
        whenever(prisonerProfileService.getPhysicalAttributes(anyString())).thenReturn(
          PrisonerPhysicalAttributesResponse(
            offenderNo = "A1234AA",
            bookings = emptyList(),
          ),
        )

        webTestClient.get().uri("/prisoners/A1234AA/physical-attributes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_PROFILE")))
          .exchange()
          .expectStatus().isOk

        verify(prisonerProfileService).getPhysicalAttributes("A1234AA")
      }
    }
  }
}
