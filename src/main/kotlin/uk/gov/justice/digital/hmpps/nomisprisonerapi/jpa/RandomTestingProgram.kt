package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.Hibernate
import java.time.LocalDate
import kotlin.jvm.javaClass

@Entity
@Table(name = "RANDOM_TESTING_PROGRAMS")
class RandomTestingProgram(
  @Id
  @Column(name = "RTP_ID", nullable = false)
  val id: Long = 0,

  @NotNull
  @Column(name = "CASELOAD_ID", nullable = false)
  val caseloadId: String = "",

  @NotNull
  @Column(name = "RTP_DATE", nullable = false)
  val rtpDate: LocalDate = LocalDate.now(),

  @NotNull
  @Column(name = "MAIN_PERCENTAGE", nullable = false)
  val mainPercentage: Short = 0,

  @NotNull
  @Column(name = "RESERVE_PERCENTAGE", nullable = false)
  val reservePercentage: Short = 0,

  @Column(name = "SELECTIONS_COUNT")
  val selectionsCount: Int? = null,

  @Column(name = "ELIGIBLE_COUNT")
  val eligibleCount: Int? = null,

  @Column(name = "RESERVE_COUNT")
  val reserveCount: Int? = null,

  @OneToMany(mappedBy = "id.randomTestingProgram", cascade = [CascadeType.ALL], fetch = LAZY)
  val offenderTestSelection: MutableList<OffenderTestSelection> = mutableListOf(),
) : NomisAuditableEntityBasic() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as RandomTestingProgram

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(id = $id )"
}
