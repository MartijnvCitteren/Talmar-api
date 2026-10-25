-- One existing vacancy to set the contact person and offer of, so the offer integration tests start
-- from a known vacancy instead of creating one through the API first. Runs before every test method
-- and clears what an earlier test left behind.
delete from vacancy_description_jpa_entity;
delete from vacancy_jpa_entity;

insert into vacancy_jpa_entity
  (id, job_title, language, category, country, city, workplace_type, min_hours, max_hours, status,
   created_at, last_modified_at)
values
  ('0199a000-0000-7000-8000-000000000011', 'Sales Manager', 'NL', 'SALES', 'NL', 'Amsterdam',
   'HYBRID', 32, 40, 'DRAFT', TIMESTAMP '2026-01-01 09:00:00', TIMESTAMP '2026-01-01 09:00:00');
