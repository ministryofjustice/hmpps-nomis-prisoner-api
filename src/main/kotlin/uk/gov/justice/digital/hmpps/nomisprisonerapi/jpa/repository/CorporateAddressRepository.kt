
package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateAddress

@Repository
interface CorporateAddressRepository : JpaRepository<CorporateAddress, Long> {
  @Suppress("ktlint:standard:function-naming")
  fun findFirstByCorporate_CorporateNameAndPremiseAndStreetAndPostalCode(corporateName: String, premise: String, street: String?, postalCode: String?): CorporateAddress?
}
