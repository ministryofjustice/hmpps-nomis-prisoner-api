CREATE OR REPLACE VIEW v_offender_visits
            (
             offender_visit_order_id,
             offender_visit_id,
             vo_offender_book_id,
             visit_offender_book_id,
             comment_text,
             override_ban_staff_id,
             search_type,
             raised_incident_type,
             raised_incident_number,
             visitor_concern_text,
             visit_date,
             start_time,
             end_time,
             visit_type,
             visit_status,
             visit_internal_location_id,
             agency_visit_slot_id,
             agy_loc_id,
             visit_order_number,
             visit_order_comment_text,
             visit_order_issue_date,
             visit_order_type,
             visit_order_status,
             visit_order_expiry_date,
             visit_order_outcome_reason,
             mailed_date,
             offender_visit_visitor_id,
             offender_book_id,
             offender_ID,
             Offender_last_Name,
             Offender_First_name,
             Offender_ID_Display,
             visit_owner_flag,
             event_status,
             event_outcome,
             outcome_reason_code,
             visitor_comment_text,
             event_id,
             check_sum)
AS
SELECT
/* MODIFICATION HISTORY
   Person     	 Date      Version     	 Comments
   ---------    ------     ---------  	 ------------------------------
   David Ng     27/06/2006 2.1           NOMIS project(10.2.0)
*/
    OVO.Offender_Visit_Order_ID,
    OV.offender_visit_id,
    OVO.Offender_Book_ID,
    OV.Offender_Book_ID Visit_Offender_Book_ID,
    OV.comment_text,
    OV.override_ban_staff_id,
    OV.search_type,
    OV.raised_incident_type,
    OV.raised_incident_number,
    OV.visitor_concern_text,
    OV.visit_date,
    OV.start_time,
    OV.end_time,
    OV.visit_type,
    OV.visit_status,
    OV.visit_internal_location_id,
    OV.agency_visit_slot_id,
    OV.agy_loc_id,
    OVO.VISIT_ORDER_NUMBER,
    OVO.COMMENT_TEXT,
    OVO.ISSUE_DATE,
    OVO.VISIT_ORDER_TYPE,
    OVO.STATUS,
    OVO.EXPIRY_DATE,
    OVO.OUTCOME_REASON_CODE,
    OVO.MAILED_DATE,
    ovv.Offender_Visit_Visitor_ID,
    OVV.offender_book_id,
    OFF.Offender_ID,
    OFF.Last_name,
    OFF.First_name,
    OFF.Offender_ID_DIsplay,
    DECODE(OVV.offender_book_id, OV.Offender_Book_ID, 'Y', 'N'),
    OVV.EVENT_STATUS,
    NVL(OVV.Event_Outcome, 'ATT'),
    OVv.outcome_reason_code,
    ovv.Comment_text,
    OVV.event_id,
    NULL -- Removed from DEV -- Tag_Schedule.check_sum(NVL(GREATEST(OVV.MODIFY_DATETIME, OV.MODIFY_DATETIME), OVV.CREATE_DATETIME))
FROM OFFENDER_VISITS OV
         JOIN OFFENDER_VISIT_VISITORS OVV ON OV.Offender_Visit_id = OVV.Offender_visit_id
         LEFT JOIN OFFENDER_VISIT_ORDERS OVO ON OVO.Offender_Visit_Order_ID = OV.Offender_visit_Order_ID
         RIGHT JOIN Offender_Bookings BKG ON BKG.Offender_Book_ID = OVV.Offender_Book_ID
         JOIN OffenderS Off ON BKG.Offender_ID = OFF.Offender_ID
WHERE OVV.Event_ID > 0
  AND NOT (OVV.Offender_Book_ID IS NULL AND OV.Visit_Date IS NOT NULL)
UNION ALL
SELECT
/* MODIFICATION HISTORY
   Person     	 Date      Version     	 Comments
   ---------    ------     ---------  	 ------------------------------
   David Ng     07/08/2006 2.2           Add Offender Details
   David Ng     27/06/2006 2.1           NOMIS project(10.2.0)
*/
    OVO.Offender_Visit_Order_ID,
    NULL,                 -- OV.offender_visit_id,
    OVO.Offender_Book_ID,
    OVO.Offender_Book_ID, -- Visit_Offender_Book_ID,
    NULL,                 -- OV.comment_text,
    NULL,                 -- OV.override_ban_staff_id,
    NULL,                 -- OV.search_type,
    NULL,                 -- OV.raised_incident_type,
    NULL,                 -- OV.raised_incident_number,
    NULL,                 -- OV.visitor_concern_text,
    NULL,                 -- OV.visit_date,
    NULL,                 -- OV.start_time,
    NULL,                 -- OV.end_time,
    NULL,                 -- OV.visit_type,
    NULL,                 -- OV.visit_status,
    NULL,                 -- OV.visit_internal_location_id,
    NULL,                 -- OV.agency_visit_slot_id,
    NULL,                 -- OV.agy_loc_id,
    OVO.VISIT_ORDER_NUMBER,
    OVO.COMMENT_TEXT,
    OVO.ISSUE_DATE,
    OVO.VISIT_ORDER_TYPE,
    OVO.STATUS,
    OVO.EXPIRY_DATE,
    OVO.OUTCOME_REASON_CODE,
    OVO.MAILED_DATE,
    NULL,                 --ovv.Offender_Visit_Visitor_ID,
    OVO.offender_book_id,
    OFF.Offender_ID,
    OFF.Last_name,
    OFF.First_name,
    OFF.Offender_ID_DIsplay,
    NULL,                 -- DECODE(OVV.offender_book_id, OV.Offender_Book_ID, 'Y', 'N'),
    NULL,                 -- OVV.EVENT_STATUS,
    NULL,                 -- OVV.Event_Outcome,
    NULL,                 -- OVv.outcome_reason_code,
    NULL,                 -- ovv.Comment_text,
    NULL,                 -- OVV.event_id,
    NULL                  -- Tag_Schedule.check_sum(NVL(GREATEST(OVO.MODIFY_DATETIME, OVO.MODIFY_DATETIME), OVO.CREATE_DATETIME))
FROM OFFENDER_VISIT_ORDERS OVO
         RIGHT JOIN
     Offender_Bookings BKG ON OVO.Offender_Book_ID = BKG.Offender_Book_ID
     JOIN OffenderS Off ON BKG.Offender_ID = OFF.Offender_ID
WHERE OVO.STATUS = 'A'
  AND NOT EXISTS
    (SELECT 'X'
     FROM OFFENDER_VISITS OV
     WHERE OV.Offender_Visit_ORder_ID = OVO.Offender_Visit_Order_ID
       AND ov.visit_status IN ('SCH', 'COMP'));

