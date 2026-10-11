# Academic hierarchy setup and backfill

The academic structure is built onto the existing model:

- `Program` continues to belong to a `Department`.
- `Semester` continues to belong to a `Program` and a term `AcademicYear`.
- `AcademicCohort` represents the student's intake/batch (for example, `2025-2029`) and belongs to one program.
- `Section` belongs to one cohort and one semester from the same program.
- `Student.cohort_id` and `Student.section_id` are nullable so existing rows remain valid until reviewed. Newly created students must have a cohort and section.
- `StudentAcademicPlacement` keeps dated placement history as a student changes semester or section. Existing assignment/submission rows continue to refer to the same student.
- A section can have only one active class-incharge assignment through the service's section lock and reassignment transaction; ended assignments are retained.

## Existing database

Back up the database before deploying. With the current `spring.jpa.hibernate.ddl-auto=update` setting, the application adds the cohort, section, placement-history, and class-incharge tables and the nullable student foreign-key columns; it does not populate students with a guessed cohort or section. Confirm this generated schema against a staging copy before production. Do not change this update to `create`, `create-drop`, or `drop`.

## Admin setup and backfill

1. Create an academic cohort for each applicable program from **Admin → Cohorts & Sections**.
2. Create sections for the cohort and matching semester. The API rejects a cohort/semester program mismatch and duplicate section names within a cohort-semester.
3. Update legacy students from **Admin → Students** with their verified cohort and section. The form keeps the existing program, department, and semester consistency checks; no legacy student is silently assigned.
4. Assign class incharges from **Admin → Class Incharges**. Reassignment closes the previous assignment and preserves its dates.
5. Confirm legacy rows are intentionally unplaced or have been reviewed before introducing any database `NOT NULL` constraint in a future migration.

Student CSV and Excel imports accept `cohortId` and `sectionId` after `semesterId`. Supply both IDs to place imported students immediately; the service validates that the cohort belongs to the selected program and the section belongs to that cohort and semester. Older files without these columns, or with both cells blank, remain supported and create intentionally unplaced records for administrator backfill. Do not supply only one of the two placement IDs.
