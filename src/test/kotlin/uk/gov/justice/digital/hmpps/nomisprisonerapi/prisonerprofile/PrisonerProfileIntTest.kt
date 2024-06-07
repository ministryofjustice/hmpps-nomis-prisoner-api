package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonerprofile

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository

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
}
