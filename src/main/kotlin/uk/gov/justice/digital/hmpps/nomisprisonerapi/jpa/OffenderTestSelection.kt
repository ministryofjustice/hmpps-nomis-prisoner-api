package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.Hibernate
import java.io.Serializable
import kotlin.jvm.javaClass

@Embeddable
data class OffenderTestSelectionId(
  @NotNull
  @Column(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBookId: Long = 0L,

  @JoinColumn(name = "RTP_ID", nullable = false)
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  val randomTestingProgram: RandomTestingProgram,
) : Serializable

@Entity
@Table(name = "OFFENDER_TEST_SELECTIONS")
class OffenderTestSelection(
  @EmbeddedId
  val id: OffenderTestSelectionId? = null,

  @NotNull
  @Column(name = "TEST_SELECTION_TYPE", nullable = false)
  val testSelectionType: String = "",

  @NotNull
  @Column(name = "TEST_SELECTION_NO", nullable = false)
  val testSelectionNo: Int = 0,

  @Column(name = "TESTED_FLAG")
  val testedFlag: String? = null,

  @Column(name = "REASON_NOT_TESTED")
  val reasonNotTested: String? = null,

  @Column(name = "NOTES")
  val notes: String? = null,
) : NomisAuditableEntityBasic() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderTestSelection

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(id = $id )"
}
