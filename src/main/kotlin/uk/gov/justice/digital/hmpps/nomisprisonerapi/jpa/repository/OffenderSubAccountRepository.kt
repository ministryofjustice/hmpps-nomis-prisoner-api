package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSubAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSubAccountId

@Repository
interface OffenderSubAccountRepository : JpaRepository<OffenderSubAccount, OffenderSubAccountId> {
  fun findByIdOffenderId(offenderId: Long): List<OffenderSubAccount>
}
