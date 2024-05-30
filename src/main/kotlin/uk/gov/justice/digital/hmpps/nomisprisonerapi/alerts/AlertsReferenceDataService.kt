package uk.gov.justice.digital.hmpps.nomisprisonerapi.alerts

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.ConflictException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@Service
@Transactional
class AlertsReferenceDataService(
  private val alertCodeRepository: ReferenceCodeRepository<AlertCode>,
  private val alertTypeRepository: ReferenceCodeRepository<AlertType>,
  private val telemetryClient: TelemetryClient,
) {
  fun createAlertCode(request: CreateAlertCode) {
    if (alertCodeRepository.existsById(AlertCode.pk(request.code))) {
      throw ConflictException("Alert code ${request.code} already exists")
    }

    if (!alertTypeRepository.existsById(AlertType.pk(request.typeCode))) {
      throw BadDataException("Alert type ${request.typeCode} does not exist")
    }

    alertCodeRepository.save(
      AlertCode(
        code = request.code,
        description = request.description,
        sequence = request.listSequence,
        parentCode = request.typeCode,
      ),
    )
    telemetryClient.trackEvent("alert-code.created", mapOf("code" to request.code), null)
  }
  fun createAlertType(request: CreateAlertType) {
    if (alertTypeRepository.existsById(AlertType.pk(request.code))) {
      throw ConflictException("Alert type ${request.code} already exists")
    }

    alertTypeRepository.save(
      AlertType(
        code = request.code,
        description = request.description,
        sequence = request.listSequence,
      ),
    )
    telemetryClient.trackEvent("alert-type.created", mapOf("code" to request.code), null)
  }
}
