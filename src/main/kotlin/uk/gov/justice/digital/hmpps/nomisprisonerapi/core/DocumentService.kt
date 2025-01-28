package uk.gov.justice.digital.hmpps.nomisprisonerapi.core

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IWPDocumentRepository

@Service
@Transactional
class DocumentService(private val documentRepository: IWPDocumentRepository) {
  fun getDocumentById(documentId: Long) = documentRepository.findByIdOrNull(documentId)?.body
    ?: throw NotFoundException("Document with id $documentId does not exist")

  fun findAllIds(bookingId: Long, templateNames: List<String>): List<DocumentIdResponse> = documentRepository.findAllDocumentIds(bookingId, templateNames).map { DocumentIdResponse(it) }
}
