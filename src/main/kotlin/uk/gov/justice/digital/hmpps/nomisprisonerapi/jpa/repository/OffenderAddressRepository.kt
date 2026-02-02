
package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress

@Repository
interface OffenderAddressRepository : JpaRepository<OffenderAddress, Long> {
  @Suppress("ktlint:standard:function-naming")
  fun findByOffender_RootOffenderId(rootOffenderId: Long): List<OffenderAddress>
}
