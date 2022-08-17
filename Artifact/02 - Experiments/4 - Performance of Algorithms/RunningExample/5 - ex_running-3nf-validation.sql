/* Inserts and Updates on the 3NF running example */

SET @TRIGGER_DISABLED = 0; /*0 means Trigger for Inserts is used*/

SET @SQL_QUERY_CACHE_TYPE = 0; 

/* Each procedure inserts records into the two tables of the 3NF decomposition*/

/*******************************/
/*U1:*/
CALL `ex_3nf`.`populate_ex_3nf_R1_alt`(250000);
CALL `ex_3nf`.`populate_ex_3nf_R2`(250000);
/*******************************/

/*Before the next update operation is executed, apply the rollback from the current
update operation to ensure the instance is back to its original state*/

/*Update events based on events*/
/*To validate the FD E->C on R1_alt, we need to 
call a stored procedure that includes a transaction 
with the same update operation followed by a validation check for the FD.
If the FD is violated, the updates are rolled back, otherwise 
the update is committed. */

/*******************************/
/*******************************/
/*U2:*/
CALL `ex_3nf`.`update-one-event-based-on-event`();
/*Point query where a specific event (Event "2") is updated
UPDATE R1_alt, R2
SET R1_alt.event = R1_alt.event + 1, R2.event = R2.event + 1
WHERE R1_alt.event=R2.event and R1_alt.event=2;*/

		/*Rollback U2:*/
		UPDATE R1_alt, R2
		SET R1_alt.event = R1_alt.event - 1, R2.event = R2.event - 1
		WHERE R1_alt.event=R2.event and R1_alt.event=3;
        
/*******************************/
/*U3:*/
CALL `ex_3nf`.`update-event-based-on-event`();
/*Corresponding group query where many events (all Event "n" where n modulo 6 leaves remainder 2) is updated
UPDATE R1_alt, R2
SET R1_alt.event = R1_alt.event + 1, R2.event = R2.event + 1
WHERE R1_alt.event=R2.event and R1_alt.event mod 6=2;*/

		/*Rollback U3:*/
		UPDATE R1_alt, R2
		SET R1_alt.event = R1_alt.event - 1, R2.event = R2.event - 1
		WHERE R1_alt.event=R2.event and R1_alt.event mod 6=3;

/*******************************/
/*******************************/
/*Update company based on event*/

/*U4:*/
CALL `ex_3nf`.`update-one-company-based-on-event`();
/*UPDATE R1_alt
SET R1_alt.company = R1_alt.company + 1
WHERE R1_alt.event=2;*/

		/*Rollback U4:*/
		UPDATE R1_alt
		SET R1_alt.company = R1_alt.company - 1
		WHERE R1_alt.event=2;

/*******************************/
/*******************************/
/*Update event based on time and venue*/

/*U5:*/
CALL `ex_3nf`.`update-one-event-based-on-time-and-venue`();
/*UPDATE R1_alt, R2
SET R1_alt.event = R1_alt.event + 1, R2.event = R2.event + 1
WHERE R1_alt.event=R2.event and R1_alt.time=R2.time and R2.time=2 and R2.venue=0;*/

		/*Rollback U5:*/
		UPDATE R1_alt, R2
		SET R1_alt.event = R1_alt.event - 1, R2.event = R2.event - 1
		WHERE R1_alt.event=R2.event and R1_alt.time=R2.time and R2.time=2 and R2.venue=0;

/*******************************/

/*U6:*/
CALL `ex_3nf`.`update-event-based-on-time-and-venue`();
/*UPDATE R1_alt, R2
SET R1_alt.event = R1_alt.event + 1, R2.event = R2.event + 1
WHERE R1_alt.event=R2.event and R1_alt.time=R2.time and R2.time mod 6=2 and R2.venue mod 2=0;*/

		/*Rollback U6:*/
		UPDATE R1_alt, R2
		SET R1_alt.event = R1_alt.event - 1, R2.event = R2.event - 1
		WHERE R1_alt.event=R2.event and R1_alt.time=R2.time and R2.time mod 6=2 and R2.venue mod 2=0;

/*******************************/
/*******************************/
/*Update event based on time and company*/

/*U7:*/
CALL `ex_3nf`.`update-one-event-based-on-time-and-company`();
/*UPDATE R1_alt, R2
SET R1_alt.event = R1_alt.event + 1, R2.event = R2.event + 1
WHERE R1_alt.event=R2.event and R1_alt.time=R2.time and R1_alt.time=4 and R1_alt.company=2;*/

		/*Rollback U7:*/
		UPDATE R1_alt, R2
		SET R1_alt.event = R1_alt.event - 1, R2.event = R2.event - 1
		WHERE R1_alt.event=R2.event and R1_alt.time=R2.time and R1_alt.time=4 and R1_alt.company=2;

/*******************************/

/*U8:*/
CALL `ex_3nf`.`update-event-based-on-time-and-company`();
/*UPDATE R1_alt, R2
SET R1_alt.event = R1_alt.event + 1, R2.event = R2.event + 1
WHERE R1_alt.event=R2.event and R1_alt.time=R2.time and R1_alt.time mod 6=4 and R1_alt.company mod 6=2;*/

		/*Rollback U8:*/
		UPDATE R1_alt, R2
		SET R1_alt.event = R1_alt.event - 1, R2.event = R2.event - 1
		WHERE R1_alt.event=R2.event and R1_alt.time=R2.time and R1_alt.time mod 6=4 and R1_alt.company mod 6=2;

/*******************************/
/*******************************/
/*Update venue based on time and company*/

/*U9:*/
CALL `ex_3nf`.`update-one-venue-based-on-time-and-company`();
/*UPDATE R1_alt, R2
SET R2.venue = R2.venue + 1
WHERE R1_alt.event=R2.event and R1_alt.time=R2.time and R1_alt.company=0 and R1_alt.time=1;*/

		/*Rollback U9:*/
		UPDATE R1_alt, R2
		SET R2.venue = R2.venue - 1
		WHERE R1_alt.event=R2.event and R1_alt.time=R2.time and R1_alt.company=0 and R1_alt.time=1;

/*******************************/
/*******************************/
/*Update time based on company and venue*/

/*U10:*/
CALL `ex_3nf`.`update-one-time-based-on-company-and-venue`();
/*UPDATE R1_alt, R2
SET R1_alt.time = R1_alt.time + 1, R2.time = R2.time + 1
WHERE R1_alt.event=R2.event and R1_alt.time=R2.time and R1_alt.company=5 and R2.venue=5;*/

		/*Rollback U10:*/
		UPDATE R1_alt, R2
		SET R1_alt.time = R1_alt.time - 1, R2.time = R2.time - 1
		WHERE R1_alt.event=R2.event and R1_alt.time=R2.time and R1_alt.company=5 and R2.venue=5;

/*******************************/
