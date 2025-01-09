package uk.gov.justice.digital.hmpps.nomisprisonerapi.coreperson

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository

@Transactional
@Service
class CorePersonService(
  private val offenderRepository: OffenderRepository,
) {
  fun getOffender(prisonNumber: String): CorePerson {
    val allOffenders = offenderRepository.findByNomsIdOrderedWithBookings(prisonNumber)
    val currentAlias = allOffenders.firstOrNull() ?: throw NotFoundException("Offender not found $prisonNumber")
    val aliases = allOffenders.drop(1)

    return currentAlias.let { o ->
      CorePerson(
        prisonNumber = o.nomsId,
        offenderId = o.id,
        title = o.title?.toCodeDescription(),
        firstName = o.firstName,
        middleName1 = o.middleName,
        middleName2 = o.middleName2,
        lastName = o.lastName,
        dateOfBirth = o.birthDate,
        birthPlace = o.birthPlace,
        race = o.ethnicity?.toCodeDescription(),
        sex = o.gender.toCodeDescription(),
        aliases = aliases.map { a ->
          Alias(
            offenderId = a.id,
            title = a.title?.toCodeDescription(),
            firstName = a.firstName,
            middleName1 = a.middleName,
            middleName2 = a.middleName2,
            lastName = a.lastName,
            dateOfBirth = a.birthDate,
            race = a.ethnicity?.toCodeDescription(),
            sex = a.gender.toCodeDescription(),
            audit = a.toAudit(),
          )
        },
        audit = o.toAudit(),
      )
    }
  }
}
