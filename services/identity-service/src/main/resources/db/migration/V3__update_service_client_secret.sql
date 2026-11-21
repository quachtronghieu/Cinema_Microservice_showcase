-- =========================================================================
-- V3__update_service_client_secret.sql
-- Database: identity_db
-- Owner: identity-service (In-charge: Nguyen Phuoc Thinh - CE190087)
-- Purpose: Set verified BCrypt hash for svc:cinema-service internal client secret
-- Matches: CINEMA_SERVICE_CLIENT_SECRET in .env.example (local demo value; change for any real deployment)
-- =========================================================================

UPDATE Service_Client
SET client_secret_hash = '$2b$12$s9Dv2OouJgwDPKh00AbAiusJ1vYvYiXr77cJ1xgvEz2CxDTdEJxYe'
WHERE client_id = 'svc:cinema-service';
