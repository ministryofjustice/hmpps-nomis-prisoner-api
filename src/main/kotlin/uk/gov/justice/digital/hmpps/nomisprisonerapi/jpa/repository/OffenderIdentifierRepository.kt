package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIdentifier
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIdentifierPK

@Repository
interface OffenderIdentifierRepository : JpaRepository<OffenderIdentifier, OffenderIdentifierPK> {
  @Query("select coalesce(max(id.sequence), 0) + 1 from OffenderIdentifier where id.offender = :offender")
  fun getNextSequence(offender: Offender): Long
}
