package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.io.Serializable
import java.time.LocalDateTime
import java.util.Objects

@Entity
@EntityOpen
@Table(name = "WORK_FLOW_LOGS")
class WorkFlowLog(
  @EmbeddedId
  val id: WorkFlowLogId,

  @ManyToOne(fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + WorkFlowAction.DOMAIN + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "WORK_ACTION_CODE", referencedColumnName = "code")),
    ],
  )
  val workActionCode: WorkFlowAction,

  @Column(name = "WORK_ACTION_DATE")
  var workActionDate: LocalDateTime? = null,

  @Column(name = "WORK_FLOW_STATUS", nullable = false)
  @Enumerated(STRING)
  var workFlowStatus: WorkFlowStatus,

  @Column(name = "CREATE_DATE")
  val createDate: LocalDateTime = LocalDateTime.now(),

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "LOCATE_AGY_LOC_ID")
  var locateAgyLoc: AgencyLocation? = null,

  // @Column(name = "ACTION_USER_ID") - always null not used
) {
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @Generated
  lateinit var createDatetime: LocalDateTime
}

@Embeddable
@EntityOpen
class WorkFlowLogId(
  @ManyToOne(optional = false, fetch = LAZY) @JoinColumn(
    name = "WORK_FLOW_ID",
    nullable = false,
  ) val workFlow: WorkFlow,

  @NotNull @Column(name = "WORK_FLOW_SEQ", nullable = false) val workFlowSeq: Int,
) : Serializable {
  override fun hashCode(): Int = Objects.hash(workFlow, workFlowSeq)
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as WorkFlowLogId

    return workFlow == other.workFlow && workFlowSeq == other.workFlowSeq
  }

  companion object {
    private const val serialVersionUID = -1429375476108526153L
  }
}

enum class WorkFlowStatus {
  DONE,
  COMP,
}
