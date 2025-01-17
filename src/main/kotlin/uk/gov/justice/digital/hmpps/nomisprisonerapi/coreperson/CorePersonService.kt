package uk.gov.justice.digital.hmpps.nomisprisonerapi.coreperson

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBeliefRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.getReleaseTime

@Transactional
@Service
class CorePersonService(
  private val offenderRepository: OffenderRepository,
  private val offenderBeliefRepository: OffenderBeliefRepository,
) {
  fun getOffender(prisonNumber: String): CorePerson {
    val allOffenders = offenderRepository.findByNomsIdOrderedWithBookings(prisonNumber)
    val currentAlias = allOffenders.firstOrNull() ?: throw NotFoundException("Offender not found $prisonNumber")
    val allBookings = currentAlias.getAllBookings()
    val latestBooking = allBookings?.firstOrNull { it.bookingSequence == 1 }

    return currentAlias.let { o ->
      CorePerson(
        prisonNumber = o.nomsId,
        inOutStatus = latestBooking?.inOutStatus ?: "OUT",
        activeFlag = latestBooking?.active ?: false,
        offenders = allOffenders.mapIndexed { i, a ->
          CoreOffender(
            offenderId = a.id,
            title = a.title?.toCodeDescription(),
            firstName = a.firstName,
            middleName1 = a.middleName,
            middleName2 = a.middleName2,
            lastName = a.lastName,
            dateOfBirth = a.birthDate,
            birthPlace = a.birthPlace,
            birthCountry = a.birthCountry?.toCodeDescription(),
            ethnicity = a.ethnicity?.toCodeDescription(),
            sex = a.gender.toCodeDescription(),
            workingName = i == 0,
          )
        },
        identifiers = allOffenders.flatMap { ao ->
          ao.identifiers.map { i ->
            Identifier(
              sequence = i.id.sequence,
              offenderId = i.id.offender.id,
              type = i.identifierType.toCodeDescription(),
              identifier = i.identifier,
              issuedAuthority = i.issuedAuthority,
              issuedDate = i.issuedDate,
              verified = i.verified ?: false,
            )
          }
        },
        sentenceStartDates = allBookings?.flatMap { b -> b.sentences.map { s -> s.startDate } }?.toSortedSet()?.toList() ?: emptyList(),
        nationalities = allBookings?.flatMap { b ->
          b.profileDetails.filter { it.id.profileType.type == "NAT" }
            .filter { it.profileCodeId != null }
            .map { n ->
              OffenderNationality(
                bookingId = b.bookingId,
                startDateTime = b.bookingBeginDate,
                endDateTime = b.getReleaseTime(),
                latestBooking = b.bookingSequence == 1,
                nationality = n.profileCode!!.toCodeDescription(),
              )
            }
        } ?: emptyList(),
        nationalityDetails = allBookings?.flatMap { b ->
          b.profileDetails.filter { it.id.profileType.type == "NATIO" }
            .filter { it.profileCodeId != null }
            .map { n ->
              OffenderNationalityDetails(
                bookingId = b.bookingId,
                details = n.profileCodeId!!,
                startDateTime = b.bookingBeginDate,
                endDateTime = b.getReleaseTime(),
                latestBooking = b.bookingSequence == 1,
              )
            }
        } ?: emptyList(),
        sexualOrientations = allBookings?.flatMap { b ->
          b.profileDetails.filter { it.id.profileType.type == "SEXO" }
            .filter { it.profileCodeId != null }
            .map { n ->
              OffenderSexualOrientation(
                bookingId = b.bookingId,
                sexualOrientation = n.profileCode!!.toCodeDescription(),
                startDateTime = b.bookingBeginDate,
                endDateTime = b.getReleaseTime(),
                latestBooking = b.bookingSequence == 1,
              )
            }
        } ?: emptyList(),
        disabilities = allBookings?.flatMap { b ->
          b.profileDetails.filter { it.id.profileType.type == "DISABILITY" }
            .filter { it.profileCodeId != null }
            .map { n ->
              OffenderDisability(
                bookingId = b.bookingId,
                disability = n.profileCodeId == "Y",
                startDateTime = b.bookingBeginDate,
                endDateTime = b.getReleaseTime(),
                latestBooking = b.bookingSequence == 1,
              )
            }
        } ?: emptyList(),
        interestsToImmigration = allBookings?.flatMap { b ->
          b.profileDetails.filter { it.id.profileType.type == "IMM" }
            .filter { it.profileCodeId != null }
            .map { n ->
              OffenderInterestToImmigration(
                bookingId = b.bookingId,
                interestToImmigration = n.profileCodeId == "Y",
                startDateTime = b.bookingBeginDate,
                endDateTime = b.getReleaseTime(),
                latestBooking = b.bookingSequence == 1,
              )
            }
        } ?: emptyList(),
        addresses = o.addresses.map { address ->
          OffenderAddress(
            addressId = address.addressId,
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
              OffenderPhoneNumber(
                phoneId = number.phoneId,
                number = number.phoneNo,
                type = number.phoneType.toCodeDescription(),
                extension = number.extNo,
              )
            },
            usages = address.usages.map { u ->
              OffenderAddressUsage(
                addressId = address.addressId,
                usage = u.addressUsage.toCodeDescription(),
                active = u.active,
              )
            },
          )
        },
        phoneNumbers = o.phones.map { number ->
          OffenderPhoneNumber(
            phoneId = number.phoneId,
            number = number.phoneNo,
            type = number.phoneType.toCodeDescription(),
            extension = number.extNo,
          )
        },
        emailAddresses = o.internetAddresses.map { address ->
          OffenderEmailAddress(
            emailAddressId = address.internetAddressId,
            email = address.internetAddress,
          )
        },
        beliefs = offenderBeliefRepository.findByRootOffenderOrderByStartDateDesc(o).map { belief ->
          OffenderBelief(
            beliefId = belief.beliefId,
            belief = belief.beliefCode.toCodeDescription(),
            startDate = belief.startDate,
            endDate = belief.endDate,
            changeReason = belief.changeReason,
            comments = belief.comments,
            verified = belief.verified ?: false,
            audit = belief.toAudit(),
          )
        },
      )
    }
  }
}
