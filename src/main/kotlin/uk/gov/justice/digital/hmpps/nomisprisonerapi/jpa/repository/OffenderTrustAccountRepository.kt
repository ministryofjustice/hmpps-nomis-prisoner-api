package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTrustAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTrustAccountId

@Repository
interface OffenderTrustAccountRepository : JpaRepository<OffenderTrustAccount, OffenderTrustAccountId>
