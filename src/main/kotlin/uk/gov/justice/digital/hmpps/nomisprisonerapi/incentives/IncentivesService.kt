package uk.gov.justice.digital.hmpps.nomisprisonerapi.incentives

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.ReferenceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incentive
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncentiveId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AvailablePrisonIepLevelRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncentiveRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.IncentiveSpecification
import java.time.LocalDateTime

@Service
@Transactional
class IncentivesService(
  private val incentiveRepository: IncentiveRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val availablePrisonIepLevelRepository: AvailablePrisonIepLevelRepository,
  private val incentiveReferenceCodeRepository: ReferenceCodeRepository<IEPLevel>,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun createIncentive(bookingId: Long, dto: CreateIncentiveRequest): CreateIncentiveResponse {
    val offenderBooking = offenderBookingRepository.findById(bookingId)
      .orElseThrow(NotFoundException(bookingId.toString()))

    val incentive = mapIncentiveModel(dto, offenderBooking)
    offenderBooking.incentives.add(incentive)

    telemetryClient.trackEvent(
      "incentive-created",
      mapOf(
        "bookingId" to incentive.id.offenderBooking.toString(),
        "sequence" to incentive.id.sequence.toString(),
        "prisonId" to incentive.location.id,
      ),
      null,
    )
    log.debug("Incentive created with Nomis id = ($bookingId, ${incentive.id.sequence})")

    return CreateIncentiveResponse(incentive.id.offenderBooking.bookingId, incentive.id.sequence)
  }

  fun findIncentiveIdsByFilter(pageRequest: Pageable, incentiveFilter: IncentiveFilter): Page<IncentiveIdResponse> {
    return incentiveRepository.findAll(IncentiveSpecification(incentiveFilter), pageRequest)
      .map { IncentiveIdResponse(bookingId = it.id.offenderBooking.bookingId, sequence = it.id.sequence) }
  }

  fun getIncentive(bookingId: Long, incentiveSequence: Long): IncentiveResponse {
    val offenderBooking = offenderBookingRepository.findByIdOrNull(bookingId)
      ?: throw NotFoundException("Offender booking $bookingId not found")
    return incentiveRepository.findByIdOrNull(
      IncentiveId(
        offenderBooking = offenderBooking,
        sequence = incentiveSequence,
      ),
    )?.let {
      // determine if this incentive is the current IEP for the booking
      val currentIep =
        it == incentiveRepository.findFirstById_offenderBookingOrderByIepDateDescId_SequenceDesc(offenderBooking)
      mapIncentiveModel(it, currentIep)
    }
      ?: throw NotFoundException("Incentive not found, booking id $bookingId, sequence $incentiveSequence")
  }

  fun getCurrentIncentive(bookingId: Long): IncentiveResponse {
    val offenderBooking = offenderBookingRepository.findByIdOrNull(bookingId)
      ?: throw NotFoundException("Offender booking $bookingId not found")
    return incentiveRepository.findFirstById_offenderBookingOrderByIepDateDescId_SequenceDesc(offenderBooking)
      ?.let {
        mapIncentiveModel(incentiveEntity = it, currentIep = true)
      } ?: throw NotFoundException("Current Incentive not found, booking id $bookingId")
  }

  fun createGlobalIncentiveLevel(createIncentiveRequest: ReferenceCode): ReferenceCode {
    return incentiveReferenceCodeRepository.findByIdOrNull(IEPLevel.pk(createIncentiveRequest.code))
      ?.let { ReferenceCode(it.code, it.domain, it.description, it.active) }
      .also { log.info("Global IEP level: $createIncentiveRequest already exists") } // TODO call update??
      ?: let {
        incentiveReferenceCodeRepository.save(
          IEPLevel(
            createIncentiveRequest.code,
            createIncentiveRequest.description,
            createIncentiveRequest.active,
          ),
        ).let { ReferenceCode(it.code, it.domain, it.description, it.active) }.also {
          telemetryClient.trackEvent(
            "global-incentive-level-updated",
            mapOf(
              "code" to it.code,
              "active" to it.active.toString(),
              "description" to it.description,
            ),
            null,
          )
        }
      }
  }

  fun updateGlobalIncentiveLevel(code: String, updateIncentiveRequest: UpdateGlobalIncentiveRequest): ReferenceCode {
    return incentiveReferenceCodeRepository.findByIdOrNull(IEPLevel.pk(code))
      ?.let {
        it.active = updateIncentiveRequest.active
        it.description = updateIncentiveRequest.description
        telemetryClient.trackEvent(
          "global-incentive-level-updated",
          mapOf(
            "code" to code,
            "active" to it.active.toString(),
            "description" to it.description,
          ),
          null,
        )
        return ReferenceCode(it.code, it.domain, it.description, it.active)
      } ?: throw NotFoundException("Incentive level: $code not found")
  }

  fun getGlobalIncentiveLevel(code: String): IEPLevel {
    return incentiveReferenceCodeRepository.findByIdOrNull(IEPLevel.pk(code))
      ?: throw NotFoundException("Incentive level: $code not found")
  }

  fun deleteGlobalIncentiveLevel(code: String) {
    incentiveReferenceCodeRepository.findByIdOrNull(IEPLevel.pk(code))
      ?.let {
        incentiveReferenceCodeRepository.delete(it)
        telemetryClient.trackEvent(
          "global-incentive-level-deleted",
          mapOf(
            "code" to code,
          ),
          null,
        )
        log.info("Global Incentive level deleted: $code")
      } ?: log.info("Global Incentive level deletion request for: $code ignored. Level does not exist")
  }

  fun reorderGlobalIncentiveLevels(codeList: List<String>) {
    codeList.mapIndexed { index, levelCode ->
      incentiveReferenceCodeRepository.findByIdOrNull(IEPLevel.pk(levelCode))?.let { iepLevel ->
        iepLevel.sequence = (index + 1)
      } ?: log.error("Attempted to reorder missing Incentive level $levelCode")
    }
    val reorderedIncentiveLevels =
      incentiveReferenceCodeRepository.findAllByDomainOrderBySequenceAsc(IEPLevel.IEP_LEVEL)
        .map { ReferenceCode(it.code, it.domain, it.description, it.active) }

    telemetryClient.trackEvent(
      "global-incentive-level-reordered",
      mapOf(
        "orderedListRequest" to codeList.toString(),
        "reorderedIncentiveLevels" to reorderedIncentiveLevels.map { it.code }.toString(),
      ),
      null,
    )
  }

  private fun mapIncentiveModel(incentiveEntity: Incentive, currentIep: Boolean): IncentiveResponse {
    return IncentiveResponse(
      bookingId = incentiveEntity.id.offenderBooking.bookingId,
      incentiveSequence = incentiveEntity.id.sequence,
      commentText = incentiveEntity.commentText,
      iepDateTime = LocalDateTime.of(incentiveEntity.iepDate, incentiveEntity.iepTime.toLocalTime()),
      iepLevel = CodeDescription(incentiveEntity.iepLevel.code, incentiveEntity.iepLevel.description),
      prisonId = incentiveEntity.location.id,
      userId = incentiveEntity.userId,
      currentIep = currentIep,
      offenderNo = incentiveEntity.id.offenderBooking.offender.nomsId,
      auditModule = incentiveEntity.auditModuleName,
      whenCreated = incentiveEntity.whenCreated,
      whenUpdated = incentiveEntity.whenUpdated,
    )
  }

  private fun mapIncentiveModel(dto: CreateIncentiveRequest, offenderBooking: OffenderBooking): Incentive {
    val sequence = offenderBooking.getNextSequence()

    val location = agencyLocationRepository.findById(dto.prisonId)
      .orElseThrow(BadDataException("Prison with id=${dto.prisonId} does not exist"))

    val availablePrisonIepLevel =
      availablePrisonIepLevelRepository.findFirstByAgencyLocationAndId(location, dto.iepLevel)
        ?: throw BadDataException("IEP type ${dto.iepLevel} does not exist for prison ${dto.prisonId}")

    return Incentive(
      id = IncentiveId(offenderBooking, sequence),
      iepLevel = availablePrisonIepLevel.iepLevel,
      iepDate = dto.iepDateTime.toLocalDate(),
      iepTime = dto.iepDateTime,
      commentText = dto.comments,
      location = location,
      userId = dto.userId,
    )
  }
}
