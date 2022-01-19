package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.AuditorAwareImpl
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalanceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SearchLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitVisitor
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuthenticationFacade::class, AuditorAwareImpl::class)
@WithMockUser
class VisitRepositoryTest {

  @Autowired
  lateinit var repository: VisitRepository

  @Autowired
  lateinit var visitVisitorRepository: VisitVisitorRepository

  @Autowired
  lateinit var offenderBookingRepository: OffenderBookingRepository

  @Autowired
  lateinit var personRepository: PersonRepository

  @Autowired
  lateinit var visitTypeRepository: ReferenceCodeRepository<VisitType>

  @Autowired
  lateinit var visitStatusRepository: ReferenceCodeRepository<VisitStatus>

  @Autowired
  lateinit var searchRepository: ReferenceCodeRepository<SearchLevel>

  @Autowired
  lateinit var agencyRepository: AgencyLocationRepository

  @Autowired
  lateinit var agencyInternalRepository: AgencyInternalLocationRepository

  @Autowired
  lateinit var offenderVisitBalanceRepository: OffenderVisitBalanceRepository

  @Autowired
  lateinit var offenderVisitBalanceAdjustmentRepository: OffenderVisitBalanceAdjustmentRepository

  @Autowired
  lateinit var entityManager: TestEntityManager

  @Test
  fun saveVisit() {
    val seedOffenderBooking = offenderBookingRepository.findById(-10L).orElseThrow()
    assertThat(seedOffenderBooking.bookingId).isEqualTo(-10L)

    val seedPerson1 = personRepository.findById(-1L).orElseThrow()
    val seedPerson2 = personRepository.findById(-2L).orElseThrow()

    val visit = Visit(
      offenderBooking = seedOffenderBooking,
      commentText = "comment text",
      visitorConcernText = "visitor concerns",
      visitDate = LocalDate.parse("2009-12-21"),
      startTime = LocalDateTime.parse("2009-12-21T13:15"),
      endTime = LocalDateTime.parse("2009-12-21T14:15"),
      visitType = visitTypeRepository.findById(VisitType.pk("SCON")).orElseThrow(),
      visitStatus = visitStatusRepository.findById(VisitStatus.pk("SCH")).orElseThrow(),
      searchLevel = searchRepository.findById(SearchLevel.pk("FULL")).orElseThrow(),
      location = agencyRepository.findById("LEI").orElseThrow(),
      agencyInternalLocation = agencyInternalRepository.findById(-3L).orElseThrow(),
    )

    visit.visitors.add(
      VisitVisitor(
        offenderBooking = seedOffenderBooking,
        visit = visit,
        person = seedPerson1,
        assistedVisit = true,
        groupLeader = true
      )
    )
    visit.visitors.add(
      VisitVisitor(
        offenderBooking = seedOffenderBooking,
        visit = visit,
        person = seedPerson2,
        eventId = visitVisitorRepository.getEventId()
      )
    )

    repository.save(visit)
    entityManager.flush()

    val persistedVisitList = repository.findByOffenderBooking(seedOffenderBooking)
    assertThat(persistedVisitList).isNotEmpty
    val persistedVisit = persistedVisitList[0]

    assertThat(persistedVisit.visitDate).isEqualTo(LocalDate.parse("2009-12-21"))
    assertThat(persistedVisit.startTime).isEqualTo(LocalDateTime.parse("2009-12-21T13:15"))
    assertThat(persistedVisit.endTime).isEqualTo(LocalDateTime.parse("2009-12-21T14:15"))
    assertThat(persistedVisit.id).isNotNull
    assertThat(persistedVisit.searchLevel!!.description).isEqualTo("Full Search")
    assertThat(persistedVisit.visitStatus!!.description).isEqualTo("Scheduled")
    assertThat(persistedVisit.visitorConcernText).isEqualTo("visitor concerns")

    val visitVisitors = persistedVisit.visitors
    assertThat(visitVisitors.size).isEqualTo(2)
    val (_, offenderBooking, parentVisit, person, groupLeader, assistedVisit) = visitVisitors[0]
    assertThat(offenderBooking?.bookingId).isEqualTo(-10L)
    assertThat(parentVisit.id).isEqualTo(visit.id)
    assertThat(groupLeader).isTrue
    assertThat(assistedVisit).isTrue
    assertThat(person?.id).isEqualTo(-1)
    assertThat(visitVisitors[1].person?.id).isEqualTo(-2)
    assertThat(visitVisitors[1].eventId).isGreaterThan(0)
  }

  @Test
  fun saveBalanceAdjustment() {
    val seedOffenderBooking = offenderBookingRepository.findById(-10L).orElseThrow()
    val seedBalance = offenderVisitBalanceRepository.findById(-10L).orElseThrow()
    assertThat(seedBalance.remainingVisitOrders).isEqualTo(25)
    assertThat(seedBalance.remainingPrivilegedVisitOrders).isEqualTo(2)

    offenderVisitBalanceAdjustmentRepository.save(
      OffenderVisitBalanceAdjustment(
        offenderBooking = seedOffenderBooking,
        adjustDate = LocalDate.of(2020, 12, 21),
        adjustReasonCode = "VO_ISSUE",
        remainingVisitOrders = -1,
        previousRemainingVisitOrders = seedBalance.remainingVisitOrders, // from offender_visit_balances
        commentText = "test comment",
        authorisedStaffId = 123L,
        endorsedStaffId = 123L,
      )
    )
    entityManager.flush()

    val offenderVisitBalanceAdjustment = offenderVisitBalanceAdjustmentRepository.findAll().first()
    assertThat(offenderVisitBalanceAdjustment.id).isEqualTo(1)
    assertThat(offenderVisitBalanceAdjustment.offenderBooking.bookingId).isEqualTo(-10)
    assertThat(offenderVisitBalanceAdjustment.adjustDate).isEqualTo(LocalDate.of(2020, 12, 21))
    assertThat(offenderVisitBalanceAdjustment.adjustReasonCode).isEqualTo("VO_ISSUE")
    assertThat(offenderVisitBalanceAdjustment.remainingVisitOrders).isEqualTo(-1)
    assertThat(offenderVisitBalanceAdjustment.previousRemainingVisitOrders).isEqualTo(25)
    assertThat(offenderVisitBalanceAdjustment.commentText).isEqualTo("test comment")
    assertThat(offenderVisitBalanceAdjustment.authorisedStaffId).isEqualTo(123L)
    assertThat(offenderVisitBalanceAdjustment.endorsedStaffId).isEqualTo(123L)
  }
}
