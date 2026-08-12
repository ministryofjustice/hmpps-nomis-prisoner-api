
package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationAddress

@Repository
interface AgencyLocationAddressRepository : JpaRepository<AgencyLocationAddress, Long> {
  // agencyLocation is a required (non-null, non-@NotFound) LAZY to-one association - without this JOIN FETCH,
  // accessing .agencyLocation on each returned address would issue one extra query per distinct agency, reintroducing
  // an N+1.
  @Query("SELECT ala FROM AgencyLocationAddress ala JOIN FETCH ala.agencyLocation WHERE ala.addressId IN :addressIds")
  fun findAllByAddressIdInWithAgencyLocation(addressIds: Collection<Long>): List<AgencyLocationAddress>
}
