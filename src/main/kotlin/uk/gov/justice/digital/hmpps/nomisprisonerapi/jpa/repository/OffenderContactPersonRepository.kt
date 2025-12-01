package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderContactPerson
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person

@Repository
interface OffenderContactPersonRepository : JpaRepository<OffenderContactPerson, Long> {

  fun findByPersonAndOffenderBooking(person: Person, booking: OffenderBooking): List<OffenderContactPerson>
}
