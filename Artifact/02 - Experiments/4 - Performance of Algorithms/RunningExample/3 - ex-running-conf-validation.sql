/* Inserts and Updates on the 2-CONF running example */

SET @SQL_QUERY_CACHE_TYPE = 0; 

/* Each procedure inserts records into the three tables of the 2-CONF decomposition
No trigger is required since the only constraints are primary keys and unique constraints
which are validated natively*/

/*******************************/
/* U1: */
CALL `ex_conf`.`populate_ex_conf_R1`(250000);
CALL `ex_conf`.`populate_ex_conf_R2`(250000);
CALL `ex_conf`.`populate_ex_conf_R3`(250000);
/*******************************/

/*CALL `ex_conf`.`insert_ex_conf_R1`(10000);
CALL `ex_conf`.`insert_ex_conf_R2`(10000);
CALL `ex_conf`.`insert_ex_conf_R3`(10000);*/

/*Before the next update operation is executed, apply the rollback from the current
update operation to ensure the instance is back to its original state*/

/*******************************/
/*******************************/

/*Update event based on event*/
/*U2: Point update where a specific event (Event "2") is updated*/
UPDATE R1, R2
SET R1.event = R1.event + 1, R2.event = R2.event + 1
WHERE R1.event=R2.event and R1.event=2;

		/*Rollback U2*/
		UPDATE R1, R2
		SET R1.event = R1.event - 1, R2.event = R2.event - 1
		WHERE R1.event=R2.event and R1.event=3;
/*******************************/

/*U3: Similar query but many events (all Event "n" where n modulo 6 leaves remainder 2) is updated*/
UPDATE R1, R2
SET R1.event = R1.event + 1, R2.event = R2.event + 1
WHERE R1.event=R2.event and R1.event mod 6=2;

		/*Rollback U3*/
		UPDATE R1, R2
		SET R1.event = R1.event - 1, R2.event = R2.event - 1
		WHERE R1.event=R2.event and R1.event mod 6=3;

/*******************************/
/*******************************/

/*Update company based on event*/
/*U4: Point update where a specific event (Event "2") is updated*/
UPDATE R1, R3
SET R1.company = R1.company + 1, R3.company = R3.company + 1
WHERE R1.event=2 and R3.company=R1.company;
 
		/*Rollback U4*/ 
		UPDATE R1, R3
		SET R1.company = R1.company - 1, R3.company = R3.company - 1
		WHERE R1.event=2 and R3.company=R1.company;
 
/*******************************/
/*******************************/

/*Update event based on time and venue*/
/*U5:*/ 
UPDATE R1, R2
SET R1.event = R1.event + 1, R2.event = R2.event + 1
WHERE R1.event=R2.event and R2.time=2 and R2.venue=0;

		/*Rollback U5*/
		UPDATE R1, R2
		SET R1.event = R1.event - 1, R2.event = R2.event - 1
		WHERE R1.event=R2.event and R2.time=2 and R2.venue=0;

/*******************************/

/*U6:*/ 
UPDATE R1, R2
SET R1.event = R1.event + 1, R2.event = R2.event + 1
WHERE R1.event=R2.event and R2.time mod 6=2 and R2.venue mod 6=0;

		/*Rollback U6*/
		UPDATE R1, R2
		SET R1.event = R1.event - 1, R2.event = R2.event - 1
		WHERE R1.event=R2.event and R2.time mod 6=2 and R2.venue mod 6=0;

/*******************************/
/*******************************/
/*Update of event based on company and time*/
/*U7:*/ 
UPDATE R1, R2
SET R1.event = R1.event + 1, R2.event = R2.event + 1
WHERE R1.company=2 and R2.time=4 and R1.event=R2.event;

		/*Rollback U7*/
		UPDATE R1, R2
		SET R1.event = R1.event - 1, R2.event = R2.event - 1
		WHERE R1.company=2 and R2.time=4 and R1.event=R2.event;

/*******************************/
/*U8:*/ 
UPDATE R1, R2
SET R1.event = R1.event + 1, R2.event = R2.event + 1
WHERE R1.company mod 6=2 and R2.time mod 6=4 and R1.event=R2.event;

		/*Rollback U8*/
		UPDATE R1, R2
		SET R1.event = R1.event - 1, R2.event = R2.event - 1
		WHERE R1.company mod 6=2 and R2.time mod 6=4 and R1.event=R2.event;

/*******************************/
/*******************************/
/*Update venue based on time and company*/
/*U9:*/ 
UPDATE R2, R3
SET R2.venue = R2.venue + 1, R3.venue = R3.venue+1
WHERE R3.company =0 and R3.time=1 and R2.time=1 and R2.venue=R3.venue;

		/*Rollback U9*/
		UPDATE R2, R3
		SET R2.venue = R2.venue - 1, R3.venue = R3.venue-1
		WHERE R3.company =0 and R3.time=1 and R2.time=1 and R2.venue=R3.venue;

/*******************************/
/*******************************/
/*Update time based on company and venue*/
/*U10:*/ 
UPDATE R2, R3
SET R2.time = R2.time + 1, R3.time = R3.time + 1
WHERE R2.time=R3.time and R2.venue=R3.venue and R3.company=5 and R3.venue=5;

		/*Rollback U10*/
		UPDATE R2, R3
		SET R2.time = R2.time - 1, R3.time = R3.time - 1
		WHERE R2.time=R3.time and R2.venue=R3.venue and R3.company=5 and R3.venue=5;

/*******************************/