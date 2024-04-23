package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.DocumentStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IWPDocument
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IWPTemplate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@DslMarker
annotation class IWPDocumentDslMarker

@NomisDataDslMarker
interface IWPDocumentDsl

@Component
class IWPDocumentBuilderFactory(
  private val repository: IWPDocumentBuilderRepository,
) {
  fun builder(): IWPDocumentBuilder {
    return IWPDocumentBuilder(repository)
  }
}

@Component
class IWPDocumentBuilderRepository(
  val documentStatusRepository: ReferenceCodeRepository<DocumentStatus>,
) {
  fun lookupDocumentStatus(code: String) = documentStatusRepository.findByIdOrNull(DocumentStatus.pk(code))!!
}

class IWPDocumentBuilder(
  private val repository: IWPDocumentBuilderRepository,

) : IWPDocumentDsl {
  fun build(
    fileName: String,
    template: IWPTemplate,
    offenderBooking: OffenderBooking,
    status: String,
    body: String?,
  ) = IWPDocument(
    fileName = fileName,
    template = template,
    offenderBooking = offenderBooking,
    status = repository.lookupDocumentStatus(status),
    body = body?.toByteArray(),
  )
}
