package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfileDetail
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfileDetailId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProfileTypeRepository

@DslMarker
annotation class OffenderProfileDetailDslMarker

@NomisDataDslMarker
interface OffenderProfileDetailDsl

@Component
class OffenderProfileDetailBuilderFactory(
  private val repository: OffenderProfileDetailBuilderRepository,
) {
  fun builder(): OffenderProfileDetailBuilder = OffenderProfileDetailBuilder(repository)
}

@Component
class OffenderProfileDetailBuilderRepository(
  val profileTypeRepository: ProfileTypeRepository,
) {
  fun lookupProfileType(type: String) = profileTypeRepository.findByIdOrNull(type)!!
}

class OffenderProfileDetailBuilder(
  private val repository: OffenderProfileDetailBuilderRepository,
) : OffenderProfileDetailDsl {

  fun build(
    listSequence: Long,
    profileTypeId: String,
    profileCodeId: String?,
    profile: OffenderProfile,
  ): OffenderProfileDetail = OffenderProfileDetail(
    id = OffenderProfileDetailId(
      offenderBooking = profile.id.offenderBooking,
      sequence = profile.id.sequence,
      profileType = repository.lookupProfileType(profileTypeId),
    ),
    offenderProfile = profile,
    profileCodeId = profileCodeId,
    listSequence = listSequence,
  )
}
