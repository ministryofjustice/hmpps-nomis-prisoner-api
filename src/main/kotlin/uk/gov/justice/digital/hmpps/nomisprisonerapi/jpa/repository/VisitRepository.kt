package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import java.util.Optional

@Repository
interface VisitRepository : CrudRepository<Visit, Long> {
  fun findByOffenderBooking(booking: OffenderBooking): List<Visit>
  fun findOneByVsipVisitId(vsipVisitId: String): Optional<Visit>
  fun findByVsipVisitId(vsipVisitId: String): List<Visit>
}
