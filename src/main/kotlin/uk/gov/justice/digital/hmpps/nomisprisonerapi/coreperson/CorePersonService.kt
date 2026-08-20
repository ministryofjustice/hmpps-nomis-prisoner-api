package uk.gov.justice.digital.hmpps.nomisprisonerapi.coreperson

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIdentifier
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIdentifierPK
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBeliefRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderIdentifierRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository

@Transactional
@Service
class CorePersonService(
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderBeliefRepository: OffenderBeliefRepository,
  private val offenderIdentifierRepository: OffenderIdentifierRepository,
) {
  fun getOffender(prisonNumber: String): CorePerson {
    val latestBooking = offenderBookingRepository.findLatestByOffenderNomsId(prisonNumber)
    val (currentAlias, rootOffender) = currentAliasAndRootOffender(prisonNumber)
    val allOffenders = offenderRepository.findByNomsId(prisonNumber).sortedBy { it.id }

    return CorePerson(
      prisonNumber = prisonNumber,
      inOutStatus = latestBooking?.inOutStatus ?: "OUT",
      activeFlag = latestBooking?.active ?: false,
      offenders = allOffenders.map {
        it.toCoreOffender(currentAlias.id)
      },
      beliefs = offenderBeliefRepository.findBeliefsByRootOffenderId(rootOffender.id)
        .map { it.toBelief() },
    )
  }

  fun getOffenderReligions(prisonNumber: String): List<OffenderBelief> = offenderBeliefRepository.findBeliefsByPrisonNumber(prisonNumber)
    .map { it.toBelief() }

  fun updateOffenderAfterMerge(prisonNumber: String, request: CorePersonMergeRequest) {
    log.info("Updating offender {} after merge", prisonNumber)
    val offender =
      offenderRepository.findRootByNomsId(prisonNumber) ?: throw NotFoundException("Offender not found $prisonNumber")
    val religionsToUpdate =
      offenderBeliefRepository.findAllById(request.religions.map { it.beliefId }).associateBy { it.beliefId }
    for ((beliefId, endDate) in request.religions) {
      val toUpdate = religionsToUpdate[beliefId] ?: throw NotFoundException("Religion not found $beliefId")
      if (toUpdate.rootOffenderId != offender.id) {
        toUpdate.rootOffenderId = offender.id
      }
      toUpdate.endDate = endDate
    }
  }

  fun getIdentifier(offenderId: Long, sequenceNumber: Int): Identifier = offenderIdentifierRepository.findById(OffenderIdentifierPK(offenderOf(offenderId), sequenceNumber.toLong()))
    .orElseThrow { NotFoundException("Identifier not found for offender $offenderId and sequence $sequenceNumber") }
    .toIdentifier()

  fun getAlias(offenderId: Long): CoreOffender {
    val offender = offenderOf(offenderId)
    val prisonNumber = offender.nomsId
    val (currentAlias, _) = currentAliasAndRootOffender(prisonNumber)
    return offender.toCoreOffender(currentAliasId = currentAlias.id, includeIdentifiers = false)
  }

  fun getOffenderAliasesAndIdentifiers(prisonNumber: String): List<CoreOffender> {
    val (currentAlias, _) = currentAliasAndRootOffender(prisonNumber)
    return offenderRepository.findByNomsId(prisonNumber).sortedBy { it.id }.map {
      it.toCoreOffender(currentAliasId = currentAlias.id, includeIdentifiers = true)
    }
  }

  fun currentAliasAndRootOffender(prisonNumber: String): CurrentAliasAndRoot {
    val rootOffender = offenderRepository.findRootByNomsId(prisonNumber)
      ?: throw NotFoundException("Offender not found $prisonNumber")
    val currentAlias =
      offenderBookingRepository.findLatestByOffenderNomsId(prisonNumber)?.offender ?: rootOffender
    return CurrentAliasAndRoot(currentAlias, rootOffender)
  }
  data class CurrentAliasAndRoot(val currentAlias: Offender, val rootOffender: Offender)

  private fun offenderOf(offenderId: Long) = offenderRepository.findById(offenderId).orElseThrow { NotFoundException("Offender not found $offenderId") }

  private fun OffenderIdentifier.toIdentifier(): Identifier = Identifier(
    sequence = id.sequence,
    type = identifierType.toCodeDescription(),
    identifier = identifier,
    issuedAuthority = issuedAuthority,
    issuedDate = issuedDate,
    verified = verified ?: false,
  )

  private fun Offender.toCoreOffender(currentAliasId: Long, includeIdentifiers: Boolean = true): CoreOffender = CoreOffender(
    offenderId = id,
    title = title?.toCodeDescription(),
    firstName = firstName,
    middleName1 = middleName,
    middleName2 = middleName2,
    lastName = lastName,
    dateOfBirth = birthDate,
    birthPlace = birthPlace,
    birthCountry = birthCountry?.toCodeDescription(),
    ethnicity = ethnicity?.toCodeDescription(),
    sex = gender.toCodeDescription(),
    nameType = nameType?.toCodeDescription(),
    createDate = createDate,
    workingName = id == currentAliasId,
    identifiers = if (includeIdentifiers) {
      identifiers.map { id ->
        Identifier(
          sequence = id.id.sequence,
          type = id.identifierType.toCodeDescription(),
          identifier = id.identifier,
          issuedAuthority = id.issuedAuthority,
          issuedDate = id.issuedDate,
          verified = id.verified ?: false,
        )
      }
    } else {
      emptyList()
    },
  )

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

private fun uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBelief.toBelief(): OffenderBelief = OffenderBelief(
  beliefId = beliefId,
  belief = beliefCode.toCodeDescription(),
  startDate = startDate,
  endDate = endDate,
  changeReason = changeReason,
  comments = comments,
  audit = toAudit(),
)
