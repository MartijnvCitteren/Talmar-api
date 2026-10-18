-- An onboarded company: acme.nl, linked to its Keycloak organization, with its founder as owner and
-- organization member. Runs before every onboarding test method and clears what an earlier test left
-- behind, users first because of the foreign key to company.
delete from app_user;
delete from company;

insert into company
  (id, name, email_domain, keycloak_organization_id, created_at, last_modified_at, last_modified_by)
values
  ('0199c9a0-0000-7000-8000-00000000a001', 'Acme', 'acme.nl', '7d4e1c2b-0000-4000-8000-00000000a0a0',
   TIMESTAMP '2026-01-01 09:00:00', TIMESTAMP '2026-01-01 09:00:00', '0199c9a0-0000-7000-8000-00000000a002');

insert into app_user
  (id, keycloak_subject, first_name, last_name, email, job_role, company_id, membership, status,
   organization_member, created_at, last_modified_at)
values
  ('0199c9a0-0000-7000-8000-00000000a002', '5b9c3f0e-0000-4000-8000-00000000a003', 'Anna', 'de Vries',
   'anna@acme.nl', 'FOUNDER_OR_DIRECTOR', '0199c9a0-0000-7000-8000-00000000a001', 'OWNER', 'ACTIVE', true,
   TIMESTAMP '2026-01-01 09:00:00', TIMESTAMP '2026-01-01 09:00:00');
