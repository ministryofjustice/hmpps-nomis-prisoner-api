package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonIdentifier
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonIdentifierPK

@Repository
interface PersonIdentifierRepository : JpaRepository<PersonIdentifier, PersonIdentifierPK>
