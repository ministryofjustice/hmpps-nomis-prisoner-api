
package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateAddress

@Repository
interface CorporateAddressRepository : JpaRepository<CorporateAddress, Long> {
  @Suppress("ktlint:standard:function-naming")
  fun findAllByCorporate_CorporateName(corporateName: String): List<CorporateAddress>

  // corporate is a required (non-null, non-@NotFound) LAZY to-one association - without this JOIN FETCH, accessing
  // .corporate on each returned address would issue one extra query per distinct corporate, reintroducing an N+1.
  @Query("SELECT ca FROM CorporateAddress ca JOIN FETCH ca.corporate WHERE ca.addressId IN :addressIds")
  fun findAllByAddressIdInWithCorporate(addressIds: Collection<Long>): List<CorporateAddress>
}
