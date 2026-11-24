package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Embeddable
class OffenderMilitaryRecordId(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID")
  val offenderBooking: OffenderBooking,

  @Column(name = "MILITARY_SEQ", nullable = false)
  val sequence: Int,
) : Serializable

@EntityOpen
@Entity
@Table(name = "OFFENDER_MILITARY_RECORDS")
class OffenderMilitaryRecord(
  @EmbeddedId
  val id: OffenderMilitaryRecordId,

  @ManyToOne
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(formula = JoinFormula(value = "'" + WarZone.MLTY_WZONE + "'", referencedColumnName = "domain")),
      JoinColumnOrFormula(column = JoinColumn(name = "WAR_ZONE_CODE", referencedColumnName = "code")),
    ],
  )
  var warZone: WarZone? = null,

  var startDate: LocalDate,

  var endDate: LocalDate? = null,

  @ManyToOne
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(formula = JoinFormula(value = "'" + MilitaryDischarge.MLTY_DSCHRG + "'", referencedColumnName = "domain")),
      JoinColumnOrFormula(column = JoinColumn(name = "MILITARY_DISCHARGE_CODE", referencedColumnName = "code")),
    ],
  )
  var militaryDischarge: MilitaryDischarge? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(formula = JoinFormula(value = "'" + MilitaryBranch.MLTY_BRANCH + "'", referencedColumnName = "domain")),
      JoinColumnOrFormula(column = JoinColumn(name = "MILITARY_BRANCH_CODE", referencedColumnName = "code")),
    ],
  )
  var militaryBranch: MilitaryBranch,

  var description: String? = null,

  var unitNumber: String? = null,

  var enlistmentLocation: String? = null,

  var dischargeLocation: String? = null,

  @Convert(converter = YesNoConverter::class)
  var selectiveServicesFlag: Boolean = false,

  @ManyToOne
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(formula = JoinFormula(value = "'" + MilitaryRank.MLTY_RANK + "'", referencedColumnName = "domain")),
      JoinColumnOrFormula(column = JoinColumn(name = "MILITARY_RANK_CODE", referencedColumnName = "code")),
    ],
  )
  var militaryRank: MilitaryRank? = null,

  var serviceNumber: String? = null,

  @ManyToOne
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(formula = JoinFormula(value = "'" + DisciplinaryAction.MLTY_DISCP + "'", referencedColumnName = "domain")),
      JoinColumnOrFormula(column = JoinColumn(name = "DISCIPLINARY_ACTION_CODE", referencedColumnName = "code")),
    ],
  )
  var disciplinaryAction: DisciplinaryAction? = null,
) {
  @Column(name = "CREATE_DATETIME")
  @Generated
  lateinit var createDatetime: LocalDateTime

  @Column(name = "CREATE_USER_ID")
  @Generated
  lateinit var createUserId: String

  @Column(name = "MODIFY_DATETIME")
  @Generated
  var modifyDatetime: LocalDateTime? = null

  @Column(name = "MODIFY_USER_ID")
  @Generated
  var modifyUserId: String? = null

  @Column(name = "AUDIT_MODULE_NAME")
  @Generated
  var auditModuleName: String? = null
}
