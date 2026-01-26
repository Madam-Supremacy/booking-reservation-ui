CREATE VIEW IF NOT EXISTS booking_history AS
SELECT
    b.id AS booking_id,
    r.name AS resource_name,
    r.type AS resource_type,
    b.start_date,
    b.end_date,
    b.booked_by,
    b.created_at
FROM bookings b
JOIN resources r ON b.resource_id = r.id
ORDER BY b.start_date DESC;
