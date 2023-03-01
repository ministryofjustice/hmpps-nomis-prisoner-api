package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import java.util.Optional

@Repository
interface VisitRepository : CrudRepository<Visit, Long>, JpaSpecificationExecutor<Visit>, VisitCustomRepository {
  fun findByOffenderBooking(booking: OffenderBooking): List<Visit>

  @Suppress("FunctionName")
  fun findByIdAndOffenderBooking_Offender_NomsId(visitId: Long, offenderNo: String): Optional<Visit>
}
