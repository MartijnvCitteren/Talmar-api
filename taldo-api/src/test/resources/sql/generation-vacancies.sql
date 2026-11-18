-- Two existing vacancies to start description generations for, so the generation integration tests
-- start from a known vacancy instead of creating one through the API first. Runs before every test
-- method and clears what an earlier test left behind.
delete from vacancy_description_generation;
delete from vacancy_description_jpa_entity;
delete from vacancy_jpa_entity;

insert into vacancy_jpa_entity
  (id, job_title, language, category, country, city, workplace_type, min_hours, max_hours, status,
   created_at, last_modified_at)
values
  ('0199a000-0000-7000-8000-000000000001', 'Sales Manager', 'NL', 'SALES', 'NL', 'Amsterdam',
   'HYBRID', 32, 40, 'DRAFT', TIMESTAMP '2026-01-01 09:00:00', TIMESTAMP '2026-01-01 09:00:00'),
  ('0199a000-0000-7000-8000-000000000002', 'Chef', 'EN', 'SALES', 'NL', 'Utrecht',
   'ONSITE', 24, 36, 'DRAFT', TIMESTAMP '2026-01-01 09:00:00', TIMESTAMP '2026-01-01 09:00:00');
