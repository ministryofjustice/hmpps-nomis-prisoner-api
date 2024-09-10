package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfileDetail
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfileId
import java.time.LocalDate

@DslMarker
annotation class OffenderProfileDslMarker

@NomisDataDslMarker
interface OffenderProfileDsl {

  @OffenderProfileDetailDslMarker
  fun detail(
    listSequence: Long = 99,
    profileType: String = "BUILD",
    profileCode: String = "SMALL",
  ): OffenderProfileDetail
}

@Component
class OffenderProfileBuilderFactory(
  private val detailBuilderFactory: OffenderProfileDetailBuilderFactory,
) {
  fun builder() = OffenderProfileBuilder(detailBuilderFactory)
}

class OffenderProfileBuilder(
  val detailBuilderFactory: OffenderProfileDetailBuilderFactory,
) : OffenderProfileDsl {
  private lateinit var offenderProfile: OffenderProfile

  fun build(
    offenderBooking: OffenderBooking,
    checkDate: LocalDate,
    sequence: Long,
  ): OffenderProfile =
    OffenderProfile(
      id = OffenderProfileId(
        offenderBooking = offenderBooking,
        sequence = sequence,
      ),
      checkDate = checkDate,
    ).also {
      offenderProfile = it
    }

  override fun detail(
    listSequence: Long,
    profileType: String,
    profileCode: String,
  ): OffenderProfileDetail =
    detailBuilderFactory.builder().build(
      profile = offenderProfile,
      listSequence = listSequence,
      profileTypeId = profileType,
      profileCodeId = profileCode,
    ).also {
      offenderProfile.profileDetails.add(it)
    }
}
