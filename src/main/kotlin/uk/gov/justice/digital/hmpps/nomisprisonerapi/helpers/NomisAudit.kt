package uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.NomisAuditableEntityBasic
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.NomisAuditableEntityWithStaff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.time.LocalDateTime

@Schema(description = "The data held in NOMIS the person or system that created this record")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisAudit(
  @Schema(description = "Date time record was created")
  val createDatetime: LocalDateTime,
  @Schema(description = "Username of person that created the record (might also be a system) ")
  val createUsername: String,
  @Schema(description = "Real name of person that created the record (might by null for system users)")
  val createDisplayName: String? = null,
  @Schema(description = "Username of person that last modified the record (might also be a system)")
  val modifyUserId: String? = null,
  @Schema(description = "Real name of person that modified the record (might by null for system users)")
  val modifyDisplayName: String? = null,
  @Schema(description = "Date time record was last modified")
  val modifyDatetime: LocalDateTime? = null,
  @Schema(description = "Audit Date time")
  val auditTimestamp: LocalDateTime? = null,
  @Schema(description = "Audit username")
  val auditUserId: String? = null,
  @Schema(description = "NOMIS or DPS module that created the record")
  val auditModuleName: String? = null,
  @Schema(description = "Client userid")
  val auditClientUserId: String? = null,
  @Schema(description = "IP Address where request originated from")
  val auditClientIpAddress: String? = null,
  @Schema(description = "Machine name where request originated from")
  val auditClientWorkstationName: String? = null,
  @Schema(description = "Additional information that is audited")
  val auditAdditionalInfo: String? = null,
)

fun NomisAuditableEntityWithStaff.toAudit() = NomisAudit(
  createDatetime = createDatetime,
  createUsername = createUsername,
  createDisplayName = createStaffUserAccount?.staff.asDisplayName(),
  modifyDatetime = modifyDatetime,
  modifyUserId = modifyUserId,
  modifyDisplayName = modifyStaffUserAccount?.staff.asDisplayName(),
  auditUserId = auditUserId,
  auditTimestamp = auditTimestamp,
  auditModuleName = auditModuleName,
  auditAdditionalInfo = auditAdditionalInfo,
  auditClientIpAddress = auditClientIpAddress,
  auditClientUserId = auditClientUserId,
  auditClientWorkstationName = auditClientWorkstationName,
)

fun NomisAuditableEntityBasic.toAudit() = NomisAudit(
  createDatetime = createDatetime,
  createUsername = createUsername,
  modifyDatetime = modifyDatetime,
  modifyUserId = modifyUserId,
  auditModuleName = auditModuleName,
)

fun Staff?.asDisplayName(): String? = this?.let { "${it.firstName} ${it.lastName}" }
