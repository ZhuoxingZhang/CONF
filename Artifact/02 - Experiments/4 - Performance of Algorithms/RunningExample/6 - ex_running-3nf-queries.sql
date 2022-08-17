/* Queries on the 3NF running example */

/* Conjunctive Queries*/
/* Q1: Which companies manage which venues */
select r.company, s.venue
from R1_alt r, R2 s
where r.event=s.event and r.time=s.time;

/* Q2: Which companies have managed an event at at venue "0"?*/
select distinct r.company
from R1_alt r, R2 s
where r.event=s.event and r.time=s.time and s.venue=0;

/* Self-join Queries*/
/* Q3: Which companies have managed some event at more than one venue?*/
select distinct r.company
from R1_alt r, R2 s1, R2 s2
where r.event=s1.event and r.time=s1.time and s1.event=s2.event and s1.venue<>s2.venue;

/* Union of Conjunctive Queries*/
/* Q4: List events managed by company 2 at venue 0 or company 5 at venue 5.*/
select r.event
from R1_alt r, R2 s
where r.event=s.event and r.time=s.time and r.company=2 and s.venue=0
union
select r.event
from R1_alt r, R2 s
where r.event=s.event and r.time=s.time and r.company=5 and s.venue=5;

/* Queries with Negation */
/* Q5: Which companies have never managed any event at more than one venue?*/
select distinct r.company 
from R1_alt r
where r.company not in (select r1.company
from R1_alt r1, R2 s1, R2 s2
where r1.event=s1.event and r1.time=s1.time and s1.event=s2.event and s1.venue<>s2.venue);

/* Universal Queries */
/* Q6: What are the events that are held at all times when company 2 holds some event at venue 0 */

select distinct r1.event
from R1_alt r1, R2 s1
where r1.event=s1.event and r1.time=s1.time and r1.company=2 and s1.venue=0 and NOT EXISTS (
	select *
    from R1_alt r2, R2 s2
    where r2.event=s2.event and r2.time=s2.time and r2.company=2 and s2.venue=0 and NOT EXISTS (
		select *
        from R1_alt r3, R2 s3
        where r3.event=s3.event and r3.time=s3.time and r3.company=2 and s3.venue=0 and r3.event=r1.event and s3.time=s2.time)); 


/* Aggregation Queries */
/* Q7: How many times do companies run events? */
select r.event, r.company, count(distinct r.time) as number_of_times
from R1_alt r
group by r.event, r.company 

/* Q8: How many times do companies run events at the same venue? */
select r.event, r.company, s.venue, count(distinct r.time) as number_of_times
from R1_alt r,R2 s
where r.event=s.event and r.time=s.time
group by r.event, r.company, s.venue

/* Q9: Which companies are the only companies that run all events at some venue? */
select r1.company 
from R1_alt r1, R2 s1, (select s.venue
from R1_alt r, R2 s
where r.event=s.event and r.time=s.time
group by s.venue
having count(distinct r.company)=1) t
where r1.event=s1.event and r1.time=s1.time and s1.venue=t.venue;

/* Q10: Which company has run the most events at the same venue? */
select distinct r.company, a2.venue, a2.num_of_events
from R1_alt r, (select r1.company, s1.venue, count(distinct r1.event) as num_of_events
                from R1_alt r1, R2 s1
                where r1.event=s1.event and r1.time=s1.time
                group by r1.company, s1.venue) a2 
where r.company=a2.company and a2.num_of_events=(
select max(a3.num_of_events) 
from (select r2.company, s2.venue, count(distinct r2.event) as num_of_events
from R1_alt r2, R2 s2
where r2.event=s2.event and r2.time=s2.time
group by r2.company, s2.venue) a3);

