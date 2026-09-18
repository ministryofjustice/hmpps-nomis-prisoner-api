package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAdvance

@Repository
interface OffenderAdvanceRepository : JpaRepository<OffenderAdvance, Long> {
  fun findByOffenderId(offenderId: Long): List<OffenderAdvance>
}
