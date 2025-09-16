package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseloadCurrentAccountsBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseloadCurrentAccountsBaseId

@Repository
interface CaseloadCurrentAccountsBaseRepository : JpaRepository<CaseloadCurrentAccountsBase, CaseloadCurrentAccountsBaseId>
