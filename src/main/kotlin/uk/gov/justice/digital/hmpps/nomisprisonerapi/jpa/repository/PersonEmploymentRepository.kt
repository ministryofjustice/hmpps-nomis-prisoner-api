package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonEmployment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonEmploymentPK

@Repository
interface PersonEmploymentRepository : JpaRepository<PersonEmployment, PersonEmploymentPK> {
  @Query("select coalesce(max(id.sequence), 0) + 1 from PersonEmployment where id.person = :person")
  fun getNextSequence(person: Person): Long
}
