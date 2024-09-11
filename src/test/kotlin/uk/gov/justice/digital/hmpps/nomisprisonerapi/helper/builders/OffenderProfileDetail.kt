package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfileDetail
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfileDetailId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProfileCodeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProfileCodeRepository
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
  val profileCodeRepository: ProfileCodeRepository,
) {
  fun lookupProfileType(type: String) = profileTypeRepository.findByIdOrNull(type)!!
  fun lookupProfileCode(type: String, code: String) = profileCodeRepository.findByIdOrNull(ProfileCodeId(type, code))
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
    profileCode = profileCodeId?.let { repository.lookupProfileCode(profileTypeId, profileCodeId) },
    profileCodeId = profileCodeId,
    listSequence = listSequence,
  )
}
