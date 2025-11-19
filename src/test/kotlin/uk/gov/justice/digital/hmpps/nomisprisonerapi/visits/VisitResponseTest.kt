package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PhoneUsage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOutcomeReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitVisitor
import uk.gov.justice.digital.hmpps.nomisprisonerapi.visits.VisitResponse.Visitor
import java.time.LocalDate
import java.time.LocalDateTime

internal class VisitResponseTest {
  lateinit var visitor: VisitVisitor
  lateinit var outcomeVisitorRecord: VisitVisitor
  val agency = AgencyLocation(id = "LEI", description = "Leeds HMP")
  val visit = Visit(
    id = 99L,
    offenderBooking = anOffenderBooking("A1234LK"),
    visitDate = LocalDate.parse("2020-01-01"),
    startDateTime = LocalDateTime.parse("2020-01-01T10:00:00"),
    endDateTime = LocalDateTime.parse("2020-01-01T11:00:00"),
    visitType = VisitType(code = "SCON", description = "Social Visit"),
    commentText = "Contact Jane 0978 5652727",
    visitorConcernText = "None",
    location = agency,
    agencyInternalLocation = AgencyInternalLocation(
      agency = agency,
      description = "LEI-VIS-01",
      locationCode = "VIS-01",
      locationType = "VIS",
    ),
    visitStatus = VisitStatus(code = "SCH", description = "Scheduled"),
    visitors = mutableListOf(),
  ).apply {
    this.createUsername = "djones"
    this.modifyUserId = "bsmith"
    this.createDatetime = LocalDateTime.parse("2020-01-01T10:00:00")
    this.modifyDatetime = LocalDateTime.parse("2020-01-01T11:00:00")
    this.visitors.add(
      VisitVisitor(
        person = Person(id = 88, firstName = "Name", lastName = "Name"),
        groupLeader = true,
        visit = this,
      ),
    )
    this.visitors.add(
      VisitVisitor(
        person = null,
        offenderBooking = this.offenderBooking,
        groupLeader = true,
        visit = this,
      ),
    )
  }

  @DisplayName("constructor")
  @Nested
  inner class Constructor {
    @BeforeEach
    internal fun setUp() {
      visitor = visit.visitors.first()
      outcomeVisitorRecord = visit.visitors.last()
    }

    @Test
    internal fun `will copy basic information about the visit`() {
      val response = VisitResponse(visit)

      assertThat(response.visitId).isEqualTo(99L)
      assertThat(response.offenderNo).isEqualTo("A1234LK")
      assertThat(response.prisonId).isEqualTo("LEI")
      assertThat(response.modifyUserId).isEqualTo("bsmith")
      assertThat(response.createUserId).isEqualTo("djones")
      assertThat(response.startDateTime).isEqualTo("2020-01-01T10:00:00")
      assertThat(response.endDateTime).isEqualTo("2020-01-01T11:00:00")
      assertThat(response.whenCreated).isEqualTo("2020-01-01T10:00:00")
      assertThat(response.whenUpdated).isEqualTo("2020-01-01T11:00:00")
      assertThat(response.visitType.code).isEqualTo("SCON")
      assertThat(response.visitStatus.code).isEqualTo("SCH")
      assertThat(response.agencyInternalLocation?.description).isEqualTo("LEI-VIS-01")
      assertThat(response.commentText).isEqualTo("Contact Jane 0978 5652727")
      assertThat(response.visitorConcernText).isEqualTo("None")
      assertThat(response.visitors).containsExactly(Visitor(personId = 88L, leadVisitor = true))
    }

    @Test
    internal fun `will have no lead visitor if not present`() {
      val theVisit = visit.copy(visitors = mutableListOf(visitor.copy(groupLeader = false), outcomeVisitorRecord)).also {
        it.createUsername = visit.createUsername
        it.modifyUserId = visit.modifyUserId
        it.createDatetime = visit.createDatetime
        it.modifyDatetime = visit.modifyDatetime
      }

      assertThat(VisitResponse(theVisit).leadVisitor).isNull()
    }

    @Test
    internal fun `will have empty phone list if there is none`() {
      val response = VisitResponse(visit)

      assertThat(response.leadVisitor?.telephones).isEmpty()
    }

    @Test
    internal fun `will have single phone in list if exits as a global number`() {
      val theVisit =
        visit.copy(
          visitors = mutableListOf(
            visitor.copy(
              groupLeader = true,
              person = Person(id = 88, firstName = "Name", lastName = "Name").apply {
                this.phones.add(
                  PersonPhone(
                    person = this,
                    phoneType = PhoneUsage("HOME", "Home"),
                    phoneNo = "0123456789",
                    extNo = "ext: 876",
                  ),
                )
              },
            ),
            outcomeVisitorRecord,
          ),
        ).also {
          it.createUsername = visit.createUsername
          it.modifyUserId = visit.modifyUserId
          it.createDatetime = visit.createDatetime
          it.modifyDatetime = visit.modifyDatetime
        }

      val response = VisitResponse(theVisit)

      assertThat(response.leadVisitor?.telephones).contains("0123456789 ext: 876")
    }

    @Test
    internal fun `will have multiple phone in list if they exist as a global numbers`() {
      val theVisit =
        visit.copy(
          visitors = mutableListOf(
            visitor.copy(
              person = Person(firstName = "Name", lastName = "Name").apply {
                this.phones.add(
                  PersonPhone(
                    person = this,
                    phoneType = PhoneUsage("HOME", "Home"),
                    phoneNo = "0123456789",
                  ).apply { createDatetime = LocalDateTime.now() },
                )
                this.phones.add(
                  PersonPhone(
                    person = this,
                    phoneType = PhoneUsage("MOBL", "Mobile"),
                    phoneNo = "07973 121212",
                  ).apply { createDatetime = LocalDateTime.now() },
                )
              },
            ),
            outcomeVisitorRecord,
          ),
        ).also {
          it.createUsername = visit.createUsername
          it.modifyUserId = visit.modifyUserId
          it.createDatetime = visit.createDatetime
          it.modifyDatetime = visit.modifyDatetime
        }
      val response = VisitResponse(theVisit)

      assertThat(response.leadVisitor?.telephones).contains("0123456789", "07973 121212")
    }

    @Test
    internal fun `will include numbers from addresses as well global numbers`() {
      val theVisitor =
        visit.copy(
          visitors = mutableListOf(
            visitor.copy(
              person = Person(firstName = "Name", lastName = "Name").apply {
                this.phones.add(
                  PersonPhone(
                    person = this,
                    phoneType = PhoneUsage("HOME", "Home"),
                    phoneNo = "0123456789",
                  ).apply { createDatetime = LocalDateTime.now() },
                )
                this.phones.add(
                  PersonPhone(
                    person = this,
                    phoneType = PhoneUsage("MOBL", "Mobile"),
                    phoneNo = "07973 121212",
                    extNo = "x777",
                  ).apply { createDatetime = LocalDateTime.now() },
                )
                this.addresses.add(
                  PersonAddress(person = this).apply {
                    phones.add(
                      AddressPhone(this, PhoneUsage("HOME", "Home"), "1234567890", null).apply { createDatetime = LocalDateTime.now() },
                    )
                    phones.add(
                      AddressPhone(this, PhoneUsage("MOBL", "Mobile"), "07973 333333", null).apply { createDatetime = LocalDateTime.now() },
                    )
                  },
                )
                this.addresses.add(
                  PersonAddress(person = this).apply {
                    phones.add(
                      AddressPhone(this, PhoneUsage("HOME", "Home"), "2345678901", "x888").apply { createDatetime = LocalDateTime.now() },
                    )
                    phones.add(
                      AddressPhone(this, PhoneUsage("MOBL", "Mobile"), "07973 444444", null).apply { createDatetime = LocalDateTime.now() },
                    )
                  },
                )
              },
            ),
            outcomeVisitorRecord,
          ),
        ).also {
          it.createUsername = visit.createUsername
          it.modifyUserId = visit.modifyUserId
          it.createDatetime = visit.createDatetime
          it.modifyDatetime = visit.modifyDatetime
        }
      val response = VisitResponse(theVisitor)

      assertThat(response.leadVisitor?.telephones).contains(
        "0123456789",
        "07973 121212 x777",
        "1234567890",
        "07973 333333",
        "2345678901 x888",
        "07973 444444",
      )
    }

    @Test
    internal fun `will order numbers by last changed`() {
      val theVisitor =
        visit.copy(
          visitors = mutableListOf(
            visitor.copy(
              person = Person(firstName = "Name", lastName = "Name").apply {
                this.phones.add(
                  PersonPhone(
                    person = this,
                    phoneType = PhoneUsage("HOME", "Home"),
                    phoneNo = "0123456789",
                  ),
                )
                this.phones.add(
                  PersonPhone(
                    person = this,
                    phoneType = PhoneUsage("MOBL", "Mobile"),
                    phoneNo = "07973 121212",
                    extNo = "x777",
                  ),
                )
                this.addresses.add(
                  PersonAddress(person = this).apply {
                    phones.add(
                      AddressPhone(this, PhoneUsage("HOME", "Home"), "1234567890", null),
                    )
                    phones.add(
                      AddressPhone(this, PhoneUsage("MOBL", "Mobile"), "07973 333333", null),
                    )
                  },
                )
                this.addresses.add(
                  PersonAddress(person = this).apply {
                    phones.add(
                      AddressPhone(this, PhoneUsage("HOME", "Home"), "2345678901", "x888"),
                    )
                    phones.add(
                      AddressPhone(this, PhoneUsage("MOBL", "Mobile"), "07973 444444", null),
                    )
                  },
                )
              }.apply {
                this.phones[0].createDatetime = LocalDateTime.now().minusDays(10) // 6th
                this.phones[1].createDatetime = LocalDateTime.now().minusDays(11)
                this.phones[1].modifyDatetime = LocalDateTime.now().minusDays(4) // 4th
                this.addresses[0].phones[0].createDatetime = LocalDateTime.now().minusDays(8) // 5th
                this.addresses[0].phones[1].createDatetime = LocalDateTime.now().minusDays(9)
                this.addresses[0].phones[1].modifyDatetime = LocalDateTime.now().minusDays(1) // 1st
                this.addresses[1].phones[0].createDatetime = LocalDateTime.now().minusDays(2) // 2nd
                this.addresses[1].phones[1].createDatetime = LocalDateTime.now().minusDays(3) // 3rd
              },
            ),
            outcomeVisitorRecord,
          ),
        ).also {
          it.createUsername = visit.createUsername
          it.modifyUserId = visit.modifyUserId
          it.createDatetime = visit.createDatetime
          it.modifyDatetime = visit.modifyDatetime
        }
      val response = VisitResponse(theVisitor)

      assertThat(response.leadVisitor?.telephones).containsExactly(
        "07973 333333",
        "2345678901 x888",
        "07973 444444",
        "07973 121212 x777",
        "1234567890",
        "0123456789",
      )
    }

    @Test
    internal fun `outcome is derived from dummy visitor record`() {
      val theVisit =
        visit.copy(
          visitors = mutableListOf(
            visitor.copy(outcomeReason = VisitOutcomeReason("BANANAS", "Visit ended due to bananas")),
            outcomeVisitorRecord.copy(outcomeReason = VisitOutcomeReason("REFUSED", "Offender Refused Visit")),
            visitor.copy(outcomeReason = VisitOutcomeReason("APPLES", "Visit ended due to apples")),
          ),
        ).also {
          it.createUsername = visit.createUsername
          it.modifyUserId = visit.modifyUserId
          it.createDatetime = visit.createDatetime
          it.modifyDatetime = visit.modifyDatetime
        }
      val response = VisitResponse(theVisit)

      assertThat(response.visitOutcome).isEqualTo(CodeDescription("REFUSED", "Offender Refused Visit"))
      assertThat(response.visitors).hasSize(2)
    }

    @Test
    internal fun `outcome is null when not present in dummy visitor record`() {
      val theVisit =
        visit.copy(
          visitors = mutableListOf(
            visitor,
            outcomeVisitorRecord.copy(outcomeReason = null),
          ),
        ).also {
          it.createUsername = visit.createUsername
          it.modifyUserId = visit.modifyUserId
          it.createDatetime = visit.createDatetime
          it.modifyDatetime = visit.modifyDatetime
        }
      val response = VisitResponse(theVisit)

      assertThat(response.visitOutcome).isNull()
    }
  }
}

fun anOffenderBooking(nomsId: String): OffenderBooking = OffenderBookingDataBuilder().build(
  offender = OffenderDataBuilder(nomsId = nomsId).build(Gender("F", "FEMALE")),
  bookingSequence = 1,
  AgencyLocation(id = "LEI", description = "Leeds HMP"),
)
