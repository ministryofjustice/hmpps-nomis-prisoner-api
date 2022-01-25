package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode.Pk
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@Service
@Transactional
class Repository(
  val genderRepository: ReferenceCodeRepository<Gender>,
  val offenderRepository: OffenderRepository,
  val offenderBookingRepository: OffenderBookingRepository,
  val offenderVisitBalanceRepository: OffenderVisitBalanceRepository,
  val agencyLocationRepository: AgencyLocationRepository,
  val personRepository: PersonRepository,
) {
  fun save(offenderBuilder: OffenderBuilder): Offender {
    val gender = lookupGender(offenderBuilder.genderCode)

    val offender = save(offenderBuilder.build(gender)).apply {
      rootOffenderId = id
    }

    offender.bookings.addAll(
      offenderBuilder.bookingBuilders.mapIndexed { index, bookingBuilder ->
        val booking = save(bookingBuilder.build(offender, index, lookupAgency(bookingBuilder.agencyLocationId)))
        bookingBuilder.visitBalanceBuilder?.run {
          save(this.build(booking))
        }
        booking
      }
    )

    return offender
  }

  fun save(personBuilder: PersonBuilder): Person = personRepository.save(personBuilder.build())
  fun save(offender: Offender): Offender = offenderRepository.saveAndFlush(offender)
  fun save(booking: OffenderBooking): OffenderBooking = offenderBookingRepository.save(booking)
  fun save(offenderVisitBalance: OffenderVisitBalance): OffenderVisitBalance =
    offenderVisitBalanceRepository.save(offenderVisitBalance)

  fun lookupGender(code: String): Gender = genderRepository.findByIdOrNull(Pk(Gender.SEX, code))!!
  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!
  fun delete(offender: Offender) = offenderRepository.deleteById(offender.id)
  fun delete(people: Collection<Person>) = personRepository.deleteAllById(people.map { it.id })
}
