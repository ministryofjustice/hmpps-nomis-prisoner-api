package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IWPDocument

@Repository
interface IWPDocumentRepository : JpaRepository<IWPDocument, Long> {
  @Query(
    """
      select document.id from IWPDocument document
      join IWPTemplate template on template.id = document.template.id
        where
          document.offenderBooking.bookingId = :bookingId
        and
          template.name in (:names)
      order by document.id asc
    """,
  )
  fun findAllDocumentIds(bookingId: Long, names: List<String>): List<Long>
}
