-- V2 (showcase copy): legacy staff/customer accounts removed (personal data).
-- Only the internal service clients and their grants are kept.
-- Add your own seed accounts for local demo (see docs/dev/local-seed/README.md).

-- -------------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM Service_Client WHERE client_id = 'svc:cinema-service')
BEGIN
    INSERT INTO Service_Client (client_id, client_secret_hash, client_name)
    VALUES ('svc:cinema-service', '$2a$12$ZqC2z5yJ1B7pI6x6T8kL6e0t1y9u8i7o6p5a4s3d2f1g0h9j8k7l6', 'Cinema Internal Service');
    
    INSERT INTO Service_Client_Grant (grant_id, client_id, target_service, scope)
    VALUES ('g-cinema-catalog', 'svc:cinema-service', 'catalog-service', 'catalog:read'),
           ('g-cinema-booking', 'svc:cinema-service', 'booking-service', 'sales:write');
END;

IF NOT EXISTS (SELECT 1 FROM Service_Client WHERE client_id = 'svc:booking-service')
BEGIN
    INSERT INTO Service_Client (client_id, client_secret_hash, client_name)
    VALUES ('svc:booking-service', '$2a$12$X1C2v3b4n5m6l7k8j9h0g9f8d7s6a5p4o3i2u1y0t9r8e7w6q5z4x', 'Booking Internal Service');
    
    INSERT INTO Service_Client_Grant (grant_id, client_id, target_service, scope)
    VALUES ('g-booking-cinema', 'svc:booking-service', 'cinema-service', 'showtimes:read'),
           ('g-booking-catalog', 'svc:booking-service', 'catalog-service', 'concessions:quote');
END;
