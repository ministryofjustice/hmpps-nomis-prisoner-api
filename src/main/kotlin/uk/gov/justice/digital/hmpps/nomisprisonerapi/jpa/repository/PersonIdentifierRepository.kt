package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonIdentifier
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonIdentifierPK

@Repository
interface PersonIdentifierRepository : JpaRepository<PersonIdentifier, PersonIdentifierPK> {
  @Query("select coalesce(max(id.sequence), 0) + 1 from PersonIdentifier where id.person = :person")
  fun getNextSequence(person: Person): Long
}
