ALTER TABLE notifications
DROP CONSTRAINT IF EXISTS notifications_reference_type_check;

ALTER TABLE notifications
    ADD CONSTRAINT notifications_reference_type_check
        CHECK (reference_type IN (
                        'COMMUNITY',
                        'DECISION',
                        'COMMENT',
                        'SYSTEM'
            ));

