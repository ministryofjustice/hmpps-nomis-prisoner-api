package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDateTime

@Entity(name = "WORK_FLOWS")
@EntityOpen
@DiscriminatorColumn(name = "OBJECT_CODE")
@Inheritance
abstract class WorkFlow(
  @SequenceGenerator(
    name = "WORK_FLOW_ID",
    sequenceName = "WORK_FLOW_ID",
    allocationSize = 1,
  )
  @GeneratedValue(generator = "WORK_FLOW_ID")
  @Id
  @Column(name = "WORK_FLOW_ID")
  val id: Long = 0,

  @OneToMany(mappedBy = "id.workFlow", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val logs: MutableList<WorkFlowLog> = mutableListOf(),
) {
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @Generated
  lateinit var createDatetime: LocalDateTime

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as WorkFlow
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  fun highestSequence() = this.logs.maxOfOrNull { it.id.workFlowSeq } ?: 0
  fun nextSequence() = this.highestSequence() + 1
}
