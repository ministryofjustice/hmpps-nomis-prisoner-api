package uk.gov.justice.digital.hmpps.nomisprisonerapi.agency

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Agency
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Prison
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PrisonRepository

@Service
@Transactional
class AgencyService(
  val agencyLocationRepository: AgencyLocationRepository,
  val agencyRepository: AgencyRepository,
  val prisonRepository: PrisonRepository,
) {

  fun getPrison(prisonId: String): PrisonResponse = prisonRepository.findByIdOrNull(prisonId)
    ?.toPrisonResponse() ?: throw NotFoundException("Prison $prisonId does not exist")
  fun getAgency(agencyId: String): AgencyResponse = agencyRepository.findByIdOrNull(agencyId)
    ?.toAgencyResponse() ?: throw NotFoundException("Agency $agencyId does not exist")
  fun getAgencyLocation(agencyId: String): AgencyLocationResponse = agencyLocationRepository.findByIdOrNull(agencyId)
    ?.toAgencyLocationResponse() ?: throw NotFoundException("Agency $agencyId does not exist")
}

fun Prison.toPrisonResponse() = PrisonResponse(
  prisonId = this.id,
  description = this.description,
  longDescription = this.longDescription,
  district = this.district?.toCodeDescription(),
  active = this.active,
  deactivationDate = this.deactivationDate,
  updateAllowed = this.updateAllowed,
  contactName = this.contactName,
  disabilityAccessCode = this.disabilityAccessCode,
  area = this.area?.toCodeDescription(),
  subArea = this.subArea?.toCodeDescription(),
  region = this.region?.toCodeDescription(),
  nomsRegion = this.nomsRegion?.toCodeDescription(),
  payrollRegion = this.payrollRegion?.toCodeDescription(),
  cjitCode = this.cjitCode,
  localAuthorities = this.localAuthorities.map { it.authority.toCodeDescription() },
  addresses = this.toAgencyAddresses(),
  phones = this.toPhoneNumbers(),
  emailAddresses = this.toEmailAddresses(),
)

fun Agency.toAgencyResponse() = AgencyResponse(
  agencyId = this.id,
  description = this.description,
  longDescription = this.longDescription,
  district = this.district?.toCodeDescription(),
  active = this.active,
  deactivationDate = this.deactivationDate,
  type = this.type.toCodeDescription(),
  updateAllowed = this.updateAllowed,
  contactName = this.contactName,
  courtType = this.courtType?.toCodeDescription(),
  disabilityAccessCode = this.disabilityAccessCode,
  area = this.area?.toCodeDescription(),
  subArea = this.subArea?.toCodeDescription(),
  region = this.region?.toCodeDescription(),
  nomsRegion = this.nomsRegion?.toCodeDescription(),
  payrollRegion = this.payrollRegion?.toCodeDescription(),
  cjitCode = this.cjitCode,
  localAuthorities = this.localAuthorities.map { it.authority.toCodeDescription() },
  addresses = this.toAgencyAddresses(),
  phones = this.toPhoneNumbers(),
  emailAddresses = this.toEmailAddresses(),
)

fun AgencyLocation.toAgencyLocationResponse() = AgencyLocationResponse(
  agencyId = this.id,
  description = this.description,
  longDescription = this.longDescription,
  active = this.active,
  deactivationDate = this.deactivationDate,
  type = this.type.toCodeDescription(),
  updateAllowed = this.updateAllowed,
  contactName = this.contactName,
  disabilityAccessCode = this.disabilityAccessCode,
  area = this.area?.toCodeDescription(),
  subArea = this.subArea?.toCodeDescription(),
  region = this.region?.toCodeDescription(),
  nomsRegion = this.nomsRegion?.toCodeDescription(),
  payrollRegion = this.payrollRegion?.toCodeDescription(),
  cjitCode = this.cjitCode,
  localAuthorities = this.localAuthorities.map { it.authority.toCodeDescription() },
  addresses = this.toAgencyAddresses(),
  phones = this.toPhoneNumbers(),
  emailAddresses = this.toEmailAddresses(),
)

fun AgencyLocation.toEmailAddresses(): List<AgencyEmailAddress> = emailAddresses.map { email ->
  AgencyEmailAddress(
    id = email.internetAddressId,
    emailAddress = email.internetAddress,
  )
}
fun AgencyLocation.toPhoneNumbers(): List<AgencyPhoneNumber> = phones.map { phone ->
  AgencyPhoneNumber(
    id = phone.phoneId,
    number = phone.phoneNo,
    extension = phone.extNo,
    type = phone.phoneType.toCodeDescription(),
  )
}
fun AgencyLocation.toAgencyAddresses(): List<AgencyAddress> = addresses.map { address ->
  AgencyAddress(
    id = address.addressId,
    type = address.addressType?.toCodeDescription(),
    flat = address.flat,
    premise = address.premise,
    street = address.street,
    locality = address.locality,
    postcode = address.postalCode,
    city = address.city?.toCodeDescription(),
    county = address.county?.toCodeDescription(),
    country = address.country?.toCodeDescription(),
    validatedPAF = address.validatedPAF,
    primaryAddress = address.primaryAddress,
    noFixedAddress = address.noFixedAddress,
    mailAddress = address.mailAddress,
    comment = address.comment,
    startDate = address.startDate,
    endDate = address.endDate,
    phoneNumbers = address.phones.map { number ->
      AgencyPhoneNumber(
        id = number.phoneId,
        number = number.phoneNo,
        type = number.phoneType.toCodeDescription(),
        extension = number.extNo,
      )
    },
  )
}
