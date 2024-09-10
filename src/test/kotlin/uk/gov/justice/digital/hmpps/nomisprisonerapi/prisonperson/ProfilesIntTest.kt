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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfileDetail
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfileDetailId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfileId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProfileCodeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProfileCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProfileTypeRepository
import java.time.LocalDate

class ProfilesIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var profileRepository: OffenderProfileRepository

  @Autowired
  private lateinit var bookingRepository: OffenderBookingRepository

  @Autowired
  private lateinit var profileTypeRepository: ProfileTypeRepository

  @Autowired
  private lateinit var profileCodeRepository: ProfileCodeRepository

  @Autowired
  private lateinit var repository: Repository

  @Nested
  inner class GetProfiles {
    private lateinit var booking: OffenderBooking

    @Test
    fun `should return profile details from the DB`() {
      val leftEyeType = profileTypeRepository.findById("L_EYE_C").get()
      val shoeSizeType = profileTypeRepository.findById("SHOESIZE").get()
      val leftEyeRed = profileCodeRepository.findById(ProfileCodeId("L_EYE_C", "RED")).get()

      nomisDataBuilder.build {
        offender(nomsId = "A1234AA") {
          booking = booking()
        }
      }
      // TODO next up - create DSL for these entities
      val profile = OffenderProfile(
        id = OffenderProfileId(
          offenderBooking = booking,
          sequence = 1L,
        ),
        checkDate = LocalDate.now(),
      ).apply {
        profileDetails.add(
          OffenderProfileDetail(
            id = OffenderProfileDetailId(
              offenderBooking = booking,
              sequence = 1L,
              profileType = leftEyeType,
            ),
            profileCode = leftEyeRed,
            offenderProfile = this,
            listSequence = 1L,
          ),
        )
        profileDetails.add(
          OffenderProfileDetail(
            id = OffenderProfileDetailId(
              offenderBooking = booking,
              sequence = 1L,
              profileType = shoeSizeType,
            ),
            profileCodeId = "8.5",
            offenderProfile = this,
            listSequence = 2L,
          ),
        )
      }
      profileRepository.save(profile)

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
