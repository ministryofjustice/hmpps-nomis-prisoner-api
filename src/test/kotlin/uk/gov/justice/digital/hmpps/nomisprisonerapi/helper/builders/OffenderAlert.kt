package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAlert
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAlertId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAlertRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@DslMarker
annotation class OffenderAlertDslMarker

@NomisDataDslMarker
interface OffenderAlertDsl

@Component
class OffenderAlertBuilderFactory(
  private val repository: OffenderAlertBuilderRepository,
) {
  fun builder(): OffenderAlertBuilder {
    return OffenderAlertBuilder(repository)
  }
}

@Component
class OffenderAlertBuilderRepository(
  private val repository: OffenderAlertRepository,
  private val alertCodeRepository: ReferenceCodeRepository<AlertCode>,
  private val alertTypeRepository: ReferenceCodeRepository<AlertType>,
) {
  fun save(courtCase: OffenderAlert): OffenderAlert =
    repository.save(courtCase)

  fun lookupAlertCode(code: String): AlertCode =
    alertCodeRepository.findByIdOrNull(AlertCode.pk(code))!!

  fun lookupAlertType(code: String): AlertType =
    alertTypeRepository.findByIdOrNull(AlertType.pk(code))!!
}

class OffenderAlertBuilder(
  private val repository: OffenderAlertBuilderRepository,
) : OffenderAlertDsl {

  fun build(
    offenderBooking: OffenderBooking,
    sequence: Long?,
    alertCode: String,
    typeCode: String,
    date: LocalDate,
    expiryDate: LocalDate?,
    authorizePersonText: String?,
    status: AlertStatus,
    commentText: String?,
    verifiedFlag: Boolean,
  ): OffenderAlert = OffenderAlert(
    id = OffenderAlertId(
      offenderBooking = offenderBooking,
      sequence = sequence ?: ((offenderBooking.alerts.maxByOrNull { it.id.sequence }?.id?.sequence ?: 0) + 1),
    ),
    alertCode = repository.lookupAlertCode(alertCode),
    alertType = repository.lookupAlertType(typeCode),
    alertDate = date,
    expiryDate = expiryDate,
    authorizePersonText = authorizePersonText,
    alertStatus = status,
    commentText = commentText,
    verifiedFlag = verifiedFlag,

  )
    .let { repository.save(it) }
}
