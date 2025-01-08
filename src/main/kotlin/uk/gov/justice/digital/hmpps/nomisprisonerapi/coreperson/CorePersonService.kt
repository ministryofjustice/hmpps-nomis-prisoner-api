package uk.gov.justice.digital.hmpps.nomisprisonerapi.coreperson

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository

@Transactional
@Service
class CorePersonService(
  private val offenderRepository: OffenderRepository,
) {
  fun getOffender(prisonNumber: String): CorePerson {
    val allOffenders = offenderRepository.findByNomsIdOrderedWithBookings(prisonNumber)
    val currentAlias = allOffenders.firstOrNull() ?: throw NotFoundException("Offender not found $prisonNumber")
    // val aliases = allOffenders.drop(1)

    return currentAlias.let {
      CorePerson(
        prisonNumber = it.nomsId,
        offenderId = it.id,
        title = it.title?.toCodeDescription(),
        firstName = it.firstName,
        middleName1 = it.middleName,
        middleName2 = it.middleName2,
        lastName = it.lastName,
        dateOfBirth = it.birthDate,
        birthPlace = it.birthPlace,
        race = it.ethnicity?.toCodeDescription(),
        sex = it.gender.toCodeDescription(),
      )
    }
  }
}
