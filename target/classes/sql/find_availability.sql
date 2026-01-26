SELECT r.id, r.name, r.type
FROM resources r
WHERE r.active = 1
AND r.id NOT IN (
    SELECT b.resource_id
    FROM bookings b
    WHERE NOT (
        :end_date <= b.start_date
        OR :start_date >= b.end_date
    )
);
