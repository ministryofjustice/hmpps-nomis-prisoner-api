package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository

class ProfilesIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var bookingRepository: OffenderBookingRepository

  @Autowired
  private lateinit var repository: Repository

  @Nested
  inner class GetProfiles {
    private lateinit var booking: OffenderBooking

    @Test
    fun `should return profile details from the DB`() {
      nomisDataBuilder.build {
        offender(nomsId = "A1234AA") {
          booking = booking {
            profile {
              detail(profileType = "L_EYE_C", profileCode = "RED")
              detail(profileType = "SHOESIZE", profileCode = "8.5")
            }
          }
        }
      }

      repository.runInTransaction {
        val profiles = bookingRepository.findLatestByOffenderNomsId("A1234AA")!!.profiles

        assertThat(profiles.first().profileDetails).extracting("id.profileType.type", "profileCodeId")
          .containsExactlyInAnyOrder(
            tuple("L_EYE_C", "RED"),
            tuple("SHOESIZE", "8.5"),
          )
      }
    }
  }
}
