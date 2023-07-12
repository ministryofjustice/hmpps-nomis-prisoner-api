package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.Pageable
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.AuditorAwareImpl
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.KeyDateAdjustmentBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.LegacyOffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.SentenceAdjustmentBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.SentenceBuilder
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuthenticationFacade::class, AuditorAwareImpl::class, Repository::class)
@WithMockUser
class AdjustmentRepositoryTest {

  @Autowired
  lateinit var builderRepository: Repository

  @Autowired
  lateinit var repository: OffenderKeyDateAdjustmentRepository

  @Autowired
  lateinit var entityManager: TestEntityManager

  private val dateTimeJan1 = LocalDateTime.of(2023, 1, 1, 10, 30)
  private val dateTimeJan5 = LocalDateTime.of(2023, 1, 5, 10, 30)
  private val dateTimeJan30 = LocalDateTime.of(2023, 1, 30, 10, 30)

  @BeforeEach
  fun setup() {
    builderRepository.save(
      LegacyOffenderBuilder()
        .withBooking(
          OffenderBookingBuilder().withKeyDateAdjustments(KeyDateAdjustmentBuilder(createdDate = dateTimeJan1))
            .withSentences(
              SentenceBuilder().withAdjustment(SentenceAdjustmentBuilder(createdDate = dateTimeJan30)),
            ),
        ),
    )

    entityManager.flush()
  }

  @Test
  fun findAdjustmentsNoDateFilter() {
    val persistedVisitList = repository.adjustmentIdsQuery_named(pageable = Pageable.ofSize(10))

    assertThat(persistedVisitList).extracting("adjustmentCategory").containsExactlyInAnyOrder("KEY_DATE", "SENTENCE")
  }

  @Test
  fun findAdjustmentsWithFromDate() {
    val persistedVisitList =
      repository.adjustmentIdsQuery_named(fromDate = dateTimeJan5.toLocalDate(), pageable = Pageable.ofSize(10))

    assertThat(persistedVisitList).extracting("adjustmentCategory").containsExactly("SENTENCE")
  }

  @Test
  fun findAdjustmentsWithToDate() {
    val persistedVisitList =
      repository.adjustmentIdsQuery_named(toDate = dateTimeJan5.toLocalDate(), pageable = Pageable.ofSize(10))

    assertThat(persistedVisitList).extracting("adjustmentCategory").containsExactly("KEY_DATE")
  }

  @Test
  fun findAdjustmentsWithDatesInclusive() {
    val persistedVisitList = repository.adjustmentIdsQuery_named(
      fromDate = dateTimeJan1.toLocalDate(),
      toDate = dateTimeJan30.toLocalDate().plusDays(1),
      pageable = Pageable.ofSize(10),
    )

    assertThat(persistedVisitList).extracting("adjustmentCategory").containsExactly("KEY_DATE", "SENTENCE")
  }

  @Test
  fun findAdjustmentsWithDatesInclusivePaged() {
    val page1 = repository.adjustmentIdsQuery_named(
      fromDate = dateTimeJan1.toLocalDate(),
      toDate = dateTimeJan30.toLocalDate().plusDays(1),
      pageable = Pageable.ofSize(1),
    )
    val page2 = repository.adjustmentIdsQuery_named(
      fromDate = dateTimeJan1.toLocalDate(),
      toDate = dateTimeJan30.toLocalDate().plusDays(1),
      pageable = Pageable.ofSize(1).withPage(1), // zero indexed
    )

    assertThat(page1.totalPages).isEqualTo(2)
    assertThat(page1.content).extracting("adjustmentCategory").containsExactly("KEY_DATE")
    assertThat(page2.content).extracting("adjustmentCategory").containsExactly("SENTENCE")
  }
}
