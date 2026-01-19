package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderContactPerson
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person

@Repository
interface OffenderContactPersonRepository : JpaRepository<OffenderContactPerson, Long> {

  fun findByPersonAndOffenderBooking(person: Person, booking: OffenderBooking): List<OffenderContactPerson>

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = "1000")])
  fun findByIdOrNullForUpdate(id: Long): OffenderContactPerson? = findByIdOrNull(id)
}
