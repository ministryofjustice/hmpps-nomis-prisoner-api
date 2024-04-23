package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IWPDocument

@Repository
interface IWPDocumentRepository : CrudRepository<IWPDocument, Long> {
  fun findByTemplateName(name: String): List<IWPDocument>
  fun findByTemplateNameStartingWith(name: String): List<IWPDocument>
  fun findByOffenderBookingBookingIdAndTemplateNameStartingWith(offenderBookingId: Long, name: String): List<IWPDocument>
  fun findByOffenderBookingBookingId(bookingId: Long): List<IWPDocument>
}
