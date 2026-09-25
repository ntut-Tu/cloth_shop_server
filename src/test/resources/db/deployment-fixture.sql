-- Test-only fixture: production/demo migrations remain unchanged.
INSERT INTO users (account, password, email, user_type, is_active)
VALUES ('deployment-fixture', 'fixture-password', 'fixture@example.invalid', 'admin', TRUE);
