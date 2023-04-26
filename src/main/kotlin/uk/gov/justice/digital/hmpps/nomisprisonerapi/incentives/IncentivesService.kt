package uk.gov.justice.digital.hmpps.nomisprisonerapi.incentives

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.audit.Audit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.ReferenceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incentive
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncentiveId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PrisonIncentiveLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PrisonIncentiveLevelId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitAllowanceLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitAllowanceLevelId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AvailablePrisonIepLevelRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncentiveRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PrisonIncentiveLevelRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitAllowanceLevelRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.IncentiveSpecification
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class IncentivesService(
  private val incentiveRepository: IncentiveRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val availablePrisonIepLevelRepository: AvailablePrisonIepLevelRepository,
  private val incentiveReferenceCodeRepository: ReferenceCodeRepository<IEPLevel>,
  private val visitAllowanceLevelsRepository: VisitAllowanceLevelRepository,
  private val prisonIncentiveLevelRepository: PrisonIncentiveLevelRepository,
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

  @Audit
  fun createGlobalIncentiveLevel(createIncentiveRequest: CreateGlobalIncentiveRequest): ReferenceCode {
    return incentiveReferenceCodeRepository.findByIdOrNull(IEPLevel.pk(createIncentiveRequest.code))
      ?.let {
        ReferenceCode(
          code = it.code,
          domain = it.domain,
          description = it.description,
          active = it.active,
          sequence = it.sequence,
          parentCode = it.parentCode,
          systemDataFlag = it.systemDataFlag,
        )
      }
      .also { log.warn("Ignoring Global IEP creation - IEP level: $createIncentiveRequest already exists") }
      ?: let {
        val nextIepLevelSequence = getNextIepLevelSequence()
        incentiveReferenceCodeRepository.save(
          IEPLevel(
            createIncentiveRequest.code,
            createIncentiveRequest.description,
            createIncentiveRequest.active,
            nextIepLevelSequence,
            expiredDate = if (createIncentiveRequest.active) null else LocalDate.now(),
          ),
        ).let {
          ReferenceCode(
            code = it.code,
            domain = it.domain,
            description = it.description,
            active = it.active,
            sequence = it.sequence,
            parentCode = it.parentCode,
            systemDataFlag = it.systemDataFlag,
          )
        }.also {
          telemetryClient.trackEvent(
            "global-incentive-level-created",
            mapOf(
              "code" to it.code,
              "active" to it.active.toString(),
              "description" to it.description,
              "sequence" to nextIepLevelSequence.toString(),
              "parentCode" to nextIepLevelSequence.toString(),
            ),
            null,
          )
        }
      }
  }

  private fun getNextIepLevelSequence() =
    incentiveReferenceCodeRepository.findAllByDomainOrderBySequenceAsc(IEPLevel.IEP_LEVEL).lastOrNull()
      ?.takeIf { it.sequence != null }?.let { maxSequenceIepLevel ->
        (maxSequenceIepLevel.sequence!! + 1)
      } ?: 1

  @Audit
  fun updateGlobalIncentiveLevel(code: String, updateIncentiveRequest: UpdateGlobalIncentiveRequest): ReferenceCode {
    return incentiveReferenceCodeRepository.findByIdOrNull(IEPLevel.pk(code))
      ?.let {
        it.expiredDate = synchroniseExpiredDateOnUpdate(updateIncentiveRequest.active, it.expiredDate)
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
        ReferenceCode(it.code, it.domain, it.description, it.active, it.sequence, it.parentCode, it.expiredDate)
      } ?: throw NotFoundException("Incentive level: $code not found")
  }

  fun synchroniseExpiredDateOnUpdate(newActiveStatus: Boolean, expiredDate: LocalDate?): LocalDate? {
    if (!newActiveStatus) {
      return expiredDate ?: LocalDate.now()
    }
    return null
  }

  fun getGlobalIncentiveLevel(code: String): ReferenceCode {
    return incentiveReferenceCodeRepository.findByIdOrNull(IEPLevel.pk(code))
      ?.let { ReferenceCode(it.code, it.domain, it.description, it.active, it.sequence, it.parentCode) }
      ?: throw NotFoundException("Incentive level: $code not found")
  }

  @Audit
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

  @Audit
  fun reorderGlobalIncentiveLevels(codeList: List<String>) {
    codeList.mapIndexed { index, levelCode ->
      incentiveReferenceCodeRepository.findByIdOrNull(IEPLevel.pk(levelCode))?.let { iepLevel ->
        iepLevel.sequence = (index + 1)
        iepLevel.parentCode = iepLevel.sequence.toString()
      } ?: log.error("Attempted to reorder missing Incentive level $levelCode")
    }
    val reorderedIncentiveLevels =
      incentiveReferenceCodeRepository.findAllByDomainOrderBySequenceAsc(IEPLevel.IEP_LEVEL)
        .map { ReferenceCode(it.code, it.domain, it.description, it.active, it.sequence, it.parentCode) }

    telemetryClient.trackEvent(
      "global-incentive-level-reordered",
      mapOf(
        "orderedListRequest" to codeList.toString(),
        "reorderedIncentiveLevels" to reorderedIncentiveLevels.map { it.code }.toString(),
      ),
      null,
    )
  }

  @Audit
  fun createPrisonIncentiveLevelData(
    prisonId: String,
    createRequest: CreatePrisonIncentiveRequest,
  ): PrisonIncentiveLevelDataResponse {
    val prison = agencyLocationRepository.findByIdOrNull(prisonId)
      ?: throw NotFoundException("Prison with id=$prisonId does not exist")

    incentiveReferenceCodeRepository.findByIdOrNull(IEPLevel.pk(createRequest.levelCode))
      ?: throw BadDataException("Incentive level with code=${createRequest.levelCode} does not exist")

    val prisonIncentiveLevel =
      savePrisonIncentiveLevel(prison, createRequest)

    val visitAllowanceLevel =
      saveVisitAllowanceLevel(prison, createRequest)

    telemetryClient.trackEvent(
      "prison-incentive-level-data-created",
      mapOf(
        "code" to createRequest.levelCode,
        "location" to prisonId,
        "active" to prisonIncentiveLevel.active.toString(),
        "defaultFlag" to prisonIncentiveLevel.default.toString(),
        "remandTransferLimit" to prisonIncentiveLevel.remandTransferLimit.toString(),
        "remandSpendLimit" to prisonIncentiveLevel.remandSpendLimit.toString(),
        "convictedTransferLimit" to prisonIncentiveLevel.convictedTransferLimit.toString(),
        "convictedSpendLimit" to prisonIncentiveLevel.convictedSpendLimit.toString(),
      ),
      null,
    )
    return mapPrisonLevelDataResponse(prisonId, createRequest.levelCode, prisonIncentiveLevel, visitAllowanceLevel)
  }

  private fun BigDecimal.toPence(): Int = this.movePointRight(2).toInt()
  private fun Int.toPounds(): BigDecimal = this.div(
    100.toFloat(),
  ).toBigDecimal()

  private fun saveVisitAllowanceLevel(
    prison: AgencyLocation,
    createRequest: CreatePrisonIncentiveRequest,
  ): VisitAllowanceLevel =
    (
      visitAllowanceLevelsRepository.findByIdOrNull(VisitAllowanceLevelId(prison, createRequest.levelCode))
        ?.let {
          log.warn("saveVisitAllowanceLevel: Updating Visit allowance as ${prison.id} ${createRequest.levelCode} already exists")
          updateVisitAllowanceLevel(
            prison = prison,
            levelCode = createRequest.levelCode,
            createRequest.toUpdateRequest(),
          )
        } ?: let {
        visitAllowanceLevelsRepository.save(
          VisitAllowanceLevel(
            id = VisitAllowanceLevelId(location = prison, iepLevelCode = createRequest.levelCode),
            visitOrderAllowance = createRequest.visitOrderAllowance,
            privilegedVisitOrderAllowance = createRequest.privilegedVisitOrderAllowance,
            active = createRequest.active,
            expiryDate = if (createRequest.active) null else LocalDate.now(),
          ),
        )
      }
      )

  private fun savePrisonIncentiveLevel(
    prison: AgencyLocation,
    createRequest: CreatePrisonIncentiveRequest,
  ): PrisonIncentiveLevel =

    prisonIncentiveLevelRepository.findByIdOrNull(PrisonIncentiveLevelId(prison, createRequest.levelCode))
      ?.also {
        log.warn("savePrisonIncentiveLevel: - Updating Prison IEP level as ${prison.id} ${createRequest.levelCode} already exists.")
        updatePrisonIncentiveLevelData(prison.id, createRequest.levelCode, createRequest.toUpdateRequest())
      }
      ?: let {
        prisonIncentiveLevelRepository.save(
          PrisonIncentiveLevel(
            id = PrisonIncentiveLevelId(location = prison, iepLevelCode = createRequest.levelCode),
            active = createRequest.active,
            default = createRequest.defaultOnAdmission,
            remandTransferLimit = createRequest.remandTransferLimitInPence?.toPounds(),
            remandSpendLimit = createRequest.remandSpendLimitInPence?.toPounds(),
            convictedTransferLimit = createRequest.convictedTransferLimitInPence?.toPounds(),
            convictedSpendLimit = createRequest.convictedSpendLimitInPence?.toPounds(),
            expiryDate = if (createRequest.active) null else LocalDate.now(),
          ),
        )
      }

  @Audit
  fun updatePrisonIncentiveLevelData(
    prisonId: String,
    levelCode: String,
    updateRequest: UpdatePrisonIncentiveRequest,
  ): PrisonIncentiveLevelDataResponse {
    val prison = agencyLocationRepository.findByIdOrNull(prisonId)
      ?: throw NotFoundException("Prison with id=$prisonId does not exist")

    incentiveReferenceCodeRepository.findByIdOrNull(IEPLevel.pk(levelCode))
      ?: throw NotFoundException("Incentive level with code=$levelCode does not exist")

    val prisonIncentiveLevel =
      updatePrisonIncentiveLevel(prison, levelCode, updateRequest)

    val visitAllowanceLevel =
      updateVisitAllowanceLevel(prison, levelCode, updateRequest)

    telemetryClient.trackEvent(
      "prison-incentive-level-data-updated",
      mapOf(
        "code" to levelCode,
        "location" to prisonId,
        "active" to prisonIncentiveLevel.active.toString(),
        "defaultFlag" to prisonIncentiveLevel.default.toString(),
        "remandTransferLimit" to prisonIncentiveLevel.remandTransferLimit.toString(),
        "remandSpendLimit" to prisonIncentiveLevel.remandSpendLimit.toString(),
        "convictedTransferLimit" to prisonIncentiveLevel.convictedTransferLimit.toString(),
        "convictedSpendLimit" to prisonIncentiveLevel.convictedSpendLimit.toString(),
      ),
      null,
    )
    return mapPrisonLevelDataResponse(prisonId, levelCode, prisonIncentiveLevel, visitAllowanceLevel)
  }

  private fun updatePrisonIncentiveLevel(
    prison: AgencyLocation,
    levelCode: String,
    updateRequest: UpdatePrisonIncentiveRequest,
  ): PrisonIncentiveLevel =
    (
      prisonIncentiveLevelRepository.findByIdOrNull(PrisonIncentiveLevelId(prison, levelCode))
        ?.also {
          it.expiryDate = synchroniseExpiredDateOnUpdate(updateRequest.active, it.expiryDate)
          it.active = updateRequest.active
          it.default = updateRequest.defaultOnAdmission
          it.remandSpendLimit = updateRequest.remandSpendLimitInPence?.toPounds()
          it.remandTransferLimit = updateRequest.remandTransferLimitInPence?.toPounds()
          it.convictedSpendLimit = updateRequest.convictedSpendLimitInPence?.toPounds()
          it.convictedTransferLimit = updateRequest.convictedTransferLimitInPence?.toPounds()
        } ?: let {
        val createRequest = updateRequest.toCreateRequest(levelCode)
        log.warn("updatePrisonIncentiveLevel: creating the PrisonIncentiveLevel as it does not exist: $createRequest")
        savePrisonIncentiveLevel(prison, createRequest)
      }
      )

  private fun updateVisitAllowanceLevel(
    prison: AgencyLocation,
    levelCode: String,
    updateRequest: UpdatePrisonIncentiveRequest,
  ): VisitAllowanceLevel =
    (
      visitAllowanceLevelsRepository.findByIdOrNull(VisitAllowanceLevelId(prison, levelCode))
        ?.also {
          it.expiryDate = synchroniseExpiredDateOnUpdate(updateRequest.active, it.expiryDate)
          it.active = updateRequest.active
          it.visitOrderAllowance = updateRequest.visitOrderAllowance
          it.privilegedVisitOrderAllowance = updateRequest.privilegedVisitOrderAllowance
        } ?: let {
        val createRequest = updateRequest.toCreateRequest(levelCode)
        log.warn("updateVisitAllowanceLevel: creating the record as it does not exist: $createRequest")
        saveVisitAllowanceLevel(prison, createRequest)
      }
      )

  @Audit
  fun deletePrisonIncentiveLevelData(prisonId: String, code: String) {
    val prison = agencyLocationRepository.findByIdOrNull(prisonId)
      ?: throw BadDataException("Prison with id=$prisonId does not exist")

    visitAllowanceLevelsRepository.findByIdOrNull(VisitAllowanceLevelId(prison, code))
      ?.let {
        visitAllowanceLevelsRepository.delete(it)
      } ?: log.info("Visit allowance level deletion request for: $code ignored. Level does not exist")

    prisonIncentiveLevelRepository.findByIdOrNull(PrisonIncentiveLevelId(prison, code))
      ?.let {
        prisonIncentiveLevelRepository.delete(it)
      } ?: log.info("Prison Incentive level deletion request for: $code ignored. Level does not exist")

    telemetryClient.trackEvent(
      "prison-incentive-level-deleted",
      mapOf(
        "code" to code,
        "prison" to prisonId,
      ),
      null,
    )
    log.info("Prison Incentive level deleted: $prisonId")
  }

  fun getPrisonIncentiveLevel(prison: String, code: String): PrisonIncentiveLevelDataResponse {
    // it is possible for Prison Incentive Level to exist without VisitAllowanceLevel but not vice versa
    val location = agencyLocationRepository.findById(prison)
      .orElseThrow(BadDataException("Prison with id=$prison does not exist"))

    val prisonIncentiveLevel =
      prisonIncentiveLevelRepository.findByIdOrNull(PrisonIncentiveLevelId(location, code))
        ?: throw NotFoundException("Prison incentive level for level $code at $prison not found")

    val visitAllowanceLevel =
      visitAllowanceLevelsRepository.findByIdOrNull(VisitAllowanceLevelId(location, code))

    return mapPrisonLevelDataResponse(prison, code, prisonIncentiveLevel, visitAllowanceLevel)
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

  private fun mapPrisonLevelDataResponse(
    prisonId: String,
    code: String,
    prisonIncentiveLevel: PrisonIncentiveLevel,
    visitAllowanceLevel: VisitAllowanceLevel?,
  ): PrisonIncentiveLevelDataResponse =
    PrisonIncentiveLevelDataResponse(
      prisonId = prisonId,
      iepLevelCode = code,
      defaultOnAdmission = prisonIncentiveLevel.default,
      active = prisonIncentiveLevel.active,
      expiryDate = prisonIncentiveLevel.expiryDate,
      remandTransferLimitInPence = prisonIncentiveLevel.remandTransferLimit?.toPence(),
      remandSpendLimitInPence = prisonIncentiveLevel.remandSpendLimit?.toPence(),
      convictedSpendLimitInPence = prisonIncentiveLevel.convictedSpendLimit?.toPence(),
      convictedTransferLimitInPence = prisonIncentiveLevel.convictedTransferLimit?.toPence(),
      visitOrderAllowance = visitAllowanceLevel?.visitOrderAllowance,
      privilegedVisitOrderAllowance = visitAllowanceLevel?.privilegedVisitOrderAllowance,
      visitAllowanceActive = visitAllowanceLevel?.active,
      visitAllowanceExpiryDate = visitAllowanceLevel?.expiryDate,
    )
}
