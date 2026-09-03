-- A physical device can host multiple installs/versions of the MFA app, each needing its own
-- secret, so the enrollment key is the app installation (appId), not the device.
ALTER TABLE totp_seed RENAME COLUMN device_id TO app_id;
ALTER TABLE totp_recovery_code RENAME COLUMN device_id TO app_id;
ALTER INDEX idx_totp_recovery_code_device_id RENAME TO idx_totp_recovery_code_app_id;
