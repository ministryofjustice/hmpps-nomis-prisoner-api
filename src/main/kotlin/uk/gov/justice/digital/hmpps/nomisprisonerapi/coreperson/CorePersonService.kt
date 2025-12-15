package uk.gov.justice.digital.hmpps.nomisprisonerapi.coreperson

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBeliefRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository

@Transactional
@Service
class CorePersonService(
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderBeliefRepository: OffenderBeliefRepository,
) {
  fun getOffender(prisonNumber: String): CorePerson {
    val latestBooking = offenderBookingRepository.findLatestByOffenderNomsId(prisonNumber)
    val rootOffender = offenderRepository.findRootByNomsId(prisonNumber) ?: throw NotFoundException("Offender not found $prisonNumber")

    // current active alias defined as the one linked to the latest booking or the root if there are no bookings
    val currentAlias = latestBooking?.offender ?: rootOffender
    val allOffenders = offenderRepository.findByNomsId(prisonNumber).sortedBy { it.id }
    val allBookings = rootOffender.getAllBookings()?.sortedBy { it.bookingSequence }

    return CorePerson(
      prisonNumber = prisonNumber,
      inOutStatus = latestBooking?.inOutStatus ?: "OUT",
      activeFlag = latestBooking?.active ?: false,
      offenders = allOffenders.map { a ->
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
          nameType = a.nameType?.toCodeDescription(),
          createDate = a.createDate,
          workingName = a.id == currentAlias.id,
          identifiers = a.identifiers.map { id ->
            Identifier(
              sequence = id.id.sequence,
              type = id.identifierType.toCodeDescription(),
              identifier = id.identifier,
              issuedAuthority = id.issuedAuthority,
              issuedDate = id.issuedDate,
              verified = id.verified ?: false,
            )
          },
        )
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
      addresses = rootOffender.addresses.map { address ->
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
          usages = address.usages.filter { u -> u.addressUsage != null }.map { u ->
            OffenderAddressUsage(
              addressId = address.addressId,
              usage = u.addressUsage!!.toCodeDescription(),
              active = u.active,
            )
          },
        )
      },
      phoneNumbers = rootOffender.phones.map { number ->
        OffenderPhoneNumber(
          phoneId = number.phoneId,
          number = number.phoneNo,
          type = number.phoneType.toCodeDescription(),
          extension = number.extNo,
        )
      },
      emailAddresses = rootOffender.internetAddresses.map { address ->
        OffenderEmailAddress(
          emailAddressId = address.internetAddressId,
          email = address.internetAddress,
        )
      },
      beliefs = offenderBeliefRepository.findByRootOffenderOrderByStartDateDesc(rootOffender).map { belief ->
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

  fun getOffenderReligions(prisonNumber: String): List<OffenderBelief> {
    val rootOffender =
      offenderRepository.findRootByNomsId(prisonNumber) ?: throw NotFoundException("Offender not found $prisonNumber")
    return offenderBeliefRepository.findByRootOffenderOrderByStartDateDesc(rootOffender).map { belief ->
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
    }
  }
}
