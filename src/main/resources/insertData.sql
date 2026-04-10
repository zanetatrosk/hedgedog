-- Seed-only script
-- Requires schema from createDb.sql

-- =====================================================
-- LOOKUP DATA
-- =====================================================
INSERT INTO skill_levels (id, name, level_order) VALUES
    (gen_random_uuid(), 'Beginner', 0),
    (gen_random_uuid(), 'Intermediate', 1),
    (gen_random_uuid(), 'Advanced', 2),
    (gen_random_uuid(), 'Professional', 3);

INSERT INTO dance_styles (id, name) VALUES
    (gen_random_uuid(), 'Bachata'),
    (gen_random_uuid(), 'Salsa'),
    (gen_random_uuid(), 'Kizomba'),
    (gen_random_uuid(), 'Zouk'),
    (gen_random_uuid(), 'Tango'),
    (gen_random_uuid(), 'West Coast Swing'),
    (gen_random_uuid(), 'Lindy Hop'),
    (gen_random_uuid(), 'Forro'),
    (gen_random_uuid(), 'Merengue'),
    (gen_random_uuid(), 'Cha-cha');

INSERT INTO currencies (code, name, symbol) VALUES
    ('USD', 'US Dollar', '$'),
    ('EUR', 'Euro', 'EUR'),
    ('CZK', 'Czech Koruna', 'CZK'),
    ('GBP', 'British Pound', 'GBP');

INSERT INTO event_types (name) VALUES
    ('Social'),
    ('Workshop'),
    ('Class'),
    ('Festival'),
    ('Competition'),
    ('Practice'),
    ('Performance'),
    ('Party'),
    ('Congress'),
    ('Bootcamp');

INSERT INTO dancer_roles (name) VALUES
    ('Leader'),
    ('Follower'),
    ('Both');

-- =====================================================
-- SAMPLE DATA FOR REGISTRATION / WAITLIST TESTING
-- =====================================================
DO $$
DECLARE
    -- Lookup IDs
    v_role_leader UUID;
    v_role_follower UUID;
    v_role_both UUID;
    v_level_beginner UUID;
    v_level_inter UUID;
    v_level_advanced UUID;
    v_style_salsa UUID;
    v_style_bachata UUID;
    v_type_party UUID;
    v_type_class UUID;

    -- User IDs
    v_user_org UUID;
    v_user_lead_1 UUID;
    v_user_follow_1 UUID;
    v_user_lead_2 UUID;
    v_user_follow_2 UUID;
    v_user_lead_3 UUID;
    v_user_follow_3 UUID;
    v_user_extra_1 UUID;
    v_user_extra_2 UUID;

    -- Event IDs
    v_event_open_limited UUID;
    v_event_couple_limited UUID;
    v_event_approval UUID;
    v_parent_series UUID;
    v_series_event UUID;

    -- Location IDs
    v_loc_prague_studio UUID;
    v_loc_brno_club UUID;
BEGIN
    -- lookups
    SELECT id INTO v_role_leader FROM dancer_roles WHERE name = 'Leader' LIMIT 1;
    SELECT id INTO v_role_follower FROM dancer_roles WHERE name = 'Follower' LIMIT 1;
    SELECT id INTO v_role_both FROM dancer_roles WHERE name = 'Both' LIMIT 1;
    SELECT id INTO v_level_beginner FROM skill_levels WHERE name = 'Beginner' LIMIT 1;
    SELECT id INTO v_level_inter FROM skill_levels WHERE name = 'Intermediate' LIMIT 1;
    SELECT id INTO v_level_advanced FROM skill_levels WHERE name = 'Advanced' LIMIT 1;
    SELECT id INTO v_style_salsa FROM dance_styles WHERE name = 'Salsa' LIMIT 1;
    SELECT id INTO v_style_bachata FROM dance_styles WHERE name = 'Bachata' LIMIT 1;
    SELECT id INTO v_type_party FROM event_types WHERE name = 'Party' LIMIT 1;
    SELECT id INTO v_type_class FROM event_types WHERE name = 'Class' LIMIT 1;

    -- users
    INSERT INTO users (email, provider, provider_id) VALUES ('organizer@example.com', 'google', 'google_org_001') RETURNING id INTO v_user_org;
    INSERT INTO users (email, provider, provider_id) VALUES ('lead1@example.com', 'google', 'google_lead_1') RETURNING id INTO v_user_lead_1;
    INSERT INTO users (email, provider, provider_id) VALUES ('follow1@example.com', 'google', 'google_follow_1') RETURNING id INTO v_user_follow_1;
    INSERT INTO users (email, provider, provider_id) VALUES ('lead2@example.com', 'google', 'google_lead_2') RETURNING id INTO v_user_lead_2;
    INSERT INTO users (email, provider, provider_id) VALUES ('follow2@example.com', 'google', 'google_follow_2') RETURNING id INTO v_user_follow_2;
    INSERT INTO users (email, provider, provider_id) VALUES ('lead3@example.com', 'google', 'google_lead_3') RETURNING id INTO v_user_lead_3;
    INSERT INTO users (email, provider, provider_id) VALUES ('follow3@example.com', 'google', 'google_follow_3') RETURNING id INTO v_user_follow_3;
    INSERT INTO users (email, provider, provider_id) VALUES ('extra1@example.com', 'google', 'google_extra_1') RETURNING id INTO v_user_extra_1;
    INSERT INTO users (email, provider, provider_id) VALUES ('extra2@example.com', 'google', 'google_extra_2') RETURNING id INTO v_user_extra_2;

    INSERT INTO user_profiles (user_id, first_name, last_name, role_id, general_skill_level_id, city, country) VALUES
        (v_user_org, 'Olivia', 'Organizer', v_role_both, v_level_advanced, 'Prague', 'Czech Republic'),
        (v_user_lead_1, 'Liam', 'LeaderOne', v_role_leader, v_level_inter, 'Prague', 'Czech Republic'),
        (v_user_follow_1, 'Fiona', 'FollowerOne', v_role_follower, v_level_inter, 'Prague', 'Czech Republic'),
        (v_user_lead_2, 'Leo', 'LeaderTwo', v_role_leader, v_level_beginner, 'Brno', 'Czech Republic'),
        (v_user_follow_2, 'Farah', 'FollowerTwo', v_role_follower, v_level_beginner, 'Brno', 'Czech Republic'),
        (v_user_lead_3, 'Lucas', 'LeaderThree', v_role_leader, v_level_advanced, 'Prague', 'Czech Republic'),
        (v_user_follow_3, 'Frida', 'FollowerThree', v_role_follower, v_level_advanced, 'Prague', 'Czech Republic'),
        (v_user_extra_1, 'Ema', 'ExtraOne', v_role_both, v_level_inter, 'Ostrava', 'Czech Republic'),
        (v_user_extra_2, 'Erik', 'ExtraTwo', v_role_both, v_level_inter, 'Ostrava', 'Czech Republic');

    -- locations
    INSERT INTO locations (name, street, house_number, city, country) VALUES
        ('Prague Salsa Studio', 'Main Street', '10', 'Prague', 'Czech Republic') RETURNING id INTO v_loc_prague_studio;
    INSERT INTO locations (name, street, house_number, city, country) VALUES
        ('Brno Dance Club', 'River Road', '5', 'Brno', 'Czech Republic') RETURNING id INTO v_loc_brno_club;

    -- event 1: open mode with limited capacity and waitlist
    INSERT INTO events (
        organizer_id, event_name, description, location_id,
        event_date, event_time, status, max_attendees
    ) VALUES (
        v_user_org, 'Open Social Limited', 'Open mode event with waitlist behavior.', v_loc_prague_studio,
        CURRENT_DATE + INTERVAL '3 days', '20:00', 'PUBLISHED', 2
    ) RETURNING id INTO v_event_open_limited;

    INSERT INTO event_registration_settings (event_id, registration_mode, allow_waitlist, require_approval)
    VALUES (v_event_open_limited, 'OPEN', true, false);

    INSERT INTO dance_styles_events (dance_style_id, event_id) VALUES (v_style_bachata, v_event_open_limited);
    INSERT INTO events_event_types (event_id, event_type_id) VALUES (v_event_open_limited, v_type_party);

    INSERT INTO registrations (event_id, user_id, role_id, status, email)
    VALUES
        (v_event_open_limited, v_user_lead_1, v_role_leader, 'REGISTERED', 'lead1@example.com'),
        (v_event_open_limited, v_user_follow_1, v_role_follower, 'REGISTERED', 'follow1@example.com');

    INSERT INTO registrations (event_id, user_id, role_id, status, email, waitlisted_at)
    VALUES
        (v_event_open_limited, v_user_extra_1, v_role_both, 'WAITLISTED', 'extra1@example.com', CURRENT_TIMESTAMP - INTERVAL '45 minutes'),
        (v_event_open_limited, v_user_extra_2, v_role_both, 'WAITLISTED', 'extra2@example.com', CURRENT_TIMESTAMP - INTERVAL '10 minutes');

    -- event 2: couple mode with limited capacity + role-balanced waitlist queue
    INSERT INTO events (
        organizer_id, event_name, description, location_id,
        event_date, event_time, status, max_attendees
    ) VALUES (
        v_user_org, 'Couple Rotation Night', 'Couple mode to test leader/follower balancing.', v_loc_brno_club,
        CURRENT_DATE + INTERVAL '7 days', '19:00', 'PUBLISHED', 4
    ) RETURNING id INTO v_event_couple_limited;

    INSERT INTO event_registration_settings (event_id, registration_mode, allow_waitlist, require_approval)
    VALUES (v_event_couple_limited, 'COUPLE', true, false);

    INSERT INTO dance_styles_events (dance_style_id, event_id) VALUES (v_style_salsa, v_event_couple_limited);
    INSERT INTO events_event_types (event_id, event_type_id) VALUES (v_event_couple_limited, v_type_class);

    -- active participants (event full)
    INSERT INTO registrations (event_id, user_id, role_id, status, email)
    VALUES
        (v_event_couple_limited, v_user_lead_1, v_role_leader, 'REGISTERED', 'lead1@example.com'),
        (v_event_couple_limited, v_user_follow_1, v_role_follower, 'REGISTERED', 'follow1@example.com'),
        (v_event_couple_limited, v_user_lead_2, v_role_leader, 'REGISTERED', 'lead2@example.com'),
        (v_event_couple_limited, v_user_follow_2, v_role_follower, 'REGISTERED', 'follow2@example.com');

    -- waitlist queue with timestamps to test fairness and role-aware promotions
    INSERT INTO registrations (event_id, user_id, role_id, status, email, waitlisted_at)
    VALUES
        (v_event_couple_limited, v_user_lead_3, v_role_leader, 'WAITLISTED', 'lead3@example.com', CURRENT_TIMESTAMP - INTERVAL '60 minutes'),
        (v_event_couple_limited, v_user_follow_3, v_role_follower, 'WAITLISTED', 'follow3@example.com', CURRENT_TIMESTAMP - INTERVAL '30 minutes');

    -- event 3: approval required
    INSERT INTO events (
        organizer_id, event_name, description, location_id,
        event_date, event_time, status, max_attendees
    ) VALUES (
        v_user_org, 'Approval Only Workshop', 'Registrations should start in pending status.', v_loc_prague_studio,
        CURRENT_DATE + INTERVAL '10 days', '18:30', 'PUBLISHED', 3
    ) RETURNING id INTO v_event_approval;

    INSERT INTO event_registration_settings (event_id, registration_mode, allow_waitlist, require_approval)
    VALUES (v_event_approval, 'OPEN', true, true);

    INSERT INTO dance_styles_events (dance_style_id, event_id) VALUES (v_style_salsa, v_event_approval);
    INSERT INTO events_event_types (event_id, event_type_id) VALUES (v_event_approval, v_type_class);

    INSERT INTO registrations (event_id, user_id, role_id, status, email)
    VALUES
        (v_event_approval, v_user_extra_1, v_role_both, 'PENDING', 'extra1@example.com'),
        (v_event_approval, v_user_extra_2, v_role_both, 'PENDING', 'extra2@example.com');

    -- recurring sample event series
    INSERT INTO event_parents (name) VALUES ('Weekly Salsa Foundations') RETURNING id INTO v_parent_series;

    FOR i IN 0..3 LOOP
        INSERT INTO events (
            parent_event_id, organizer_id, event_name, description, location_id,
            event_date, event_time, status
        ) VALUES (
            v_parent_series,
            v_user_org,
            'Weekly Salsa Foundations (Week ' || (i + 1) || ')',
            'Series sample for timeline and grouping checks.',
            v_loc_prague_studio,
            CURRENT_DATE + INTERVAL '2 days' + (i * 7 || ' days')::INTERVAL,
            '18:00',
            'PUBLISHED'
        ) RETURNING id INTO v_series_event;

        INSERT INTO dance_styles_events (dance_style_id, event_id) VALUES (v_style_salsa, v_series_event);
        INSERT INTO events_event_types (event_id, event_type_id) VALUES (v_series_event, v_type_class);
    END LOOP;
END $$;

