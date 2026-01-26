-- Remove conflicting tables
DROP TABLE IF EXISTS dance_styles CASCADE;
DROP TABLE IF EXISTS media CASCADE;
DROP TABLE IF EXISTS event_registration CASCADE;
DROP TABLE IF EXISTS event_type CASCADE;
DROP TABLE IF EXISTS events CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS dancer_role CASCADE;
DROP TABLE IF EXISTS skill_levels CASCADE;
DROP TABLE IF EXISTS user_profiles CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS user_favorites CASCADE;
DROP TABLE IF EXISTS dance_styles_events CASCADE;
DROP TABLE IF EXISTS events_skill_levels CASCADE;
DROP TABLE IF EXISTS notifications_users CASCADE;
DROP TABLE IF EXISTS user_dance_interests CASCADE;
DROP TABLE IF EXISTS user_dance_styles CASCADE;
DROP TABLE IF EXISTS events_event_types CASCADE;
DROP TABLE IF EXISTS currencies CASCADE;
DROP TABLE IF EXISTS event_parent CASCADE;
DROP TABLE IF EXISTS events_media CASCADE;
DROP TABLE IF EXISTS user_media CASCADE;
DROP TABLE IF EXISTS profile_media CASCADE;
DROP TABLE IF EXISTS event_media CASCADE;
DROP TABLE IF EXISTS locations CASCADE;
-- End of removing

-- Users table
CREATE TABLE users (
                       id UUID NOT NULL DEFAULT gen_random_uuid(),
                       email VARCHAR(255) NOT NULL UNIQUE,
                       provider VARCHAR(50) NOT NULL,
                       provider_id VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       last_login_at TIMESTAMP WITH TIME ZONE
);
ALTER TABLE users ADD CONSTRAINT pk_users PRIMARY KEY (id);

-- User profiles table
CREATE TABLE user_profiles (
                               user_id UUID NOT NULL,
                               first_name VARCHAR(100),
                               last_name VARCHAR(100),
                               bio TEXT,
                               role_id UUID,
                               general_skill_level_id UUID,
                               city VARCHAR(100),
                               country VARCHAR(100),
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE user_profiles ADD CONSTRAINT pk_user_profiles PRIMARY KEY (user_id);

-- Role table (Leader/Follower/Both)
CREATE TABLE dancer_role (
                             id UUID NOT NULL DEFAULT gen_random_uuid(),
                             name VARCHAR(50) NOT NULL
);
ALTER TABLE dancer_role ADD CONSTRAINT pk_dancer_role PRIMARY KEY (id);
-- Dance styles lookup table
CREATE TABLE dance_styles (
                              id UUID NOT NULL DEFAULT gen_random_uuid(),
                              name VARCHAR(100) NOT NULL,
                              created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE dance_styles ADD CONSTRAINT pk_dance_styles PRIMARY KEY (id);
ALTER TABLE dance_styles ADD CONSTRAINT uc_dance_styles_name UNIQUE (name);

-- Currencies lookup table
CREATE TABLE currencies (
                            code VARCHAR(3) NOT NULL,
                            name VARCHAR(50) NOT NULL,
                            symbol VARCHAR(10),
                            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE currencies ADD CONSTRAINT pk_currencies PRIMARY KEY (code);

-- Skill levels lookup table
CREATE TABLE skill_levels (
                              id UUID NOT NULL DEFAULT gen_random_uuid(),
                              name VARCHAR(50) NOT NULL,
                              level_order INTEGER NOT NULL,
                              created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE skill_levels ADD CONSTRAINT pk_skill_levels PRIMARY KEY (id);
ALTER TABLE skill_levels ADD CONSTRAINT uc_skill_levels_name UNIQUE (name);
ALTER TABLE skill_levels ADD CONSTRAINT uc_skill_levels_order UNIQUE (level_order);

-- Event types lookup table
CREATE TABLE event_type (
                            id UUID NOT NULL DEFAULT gen_random_uuid(),
                            name VARCHAR(100) NOT NULL,
                            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE event_type ADD CONSTRAINT pk_event_type PRIMARY KEY (id);
ALTER TABLE event_type ADD CONSTRAINT uc_event_type_name UNIQUE (name);

-- Event parent table (for recurring events)
CREATE TABLE event_parent (
                              id UUID NOT NULL DEFAULT gen_random_uuid(),
                              name VARCHAR(255) NOT NULL
);
ALTER TABLE event_parent ADD CONSTRAINT pk_event_parent PRIMARY KEY (id);

-- Location table
CREATE TABLE locations (
                           id UUID NOT NULL DEFAULT gen_random_uuid(),
                           name VARCHAR(255),
                           street VARCHAR(255),
                           house_number VARCHAR(20),
                           city VARCHAR(100) NOT NULL,
                           state VARCHAR(100),
                           county VARCHAR(100),
                           postal_code VARCHAR(20),
                           country VARCHAR(100) NOT NULL,
                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE locations ADD CONSTRAINT pk_locations PRIMARY KEY (id);

-- Events table
CREATE TABLE events (
                        id UUID NOT NULL DEFAULT gen_random_uuid(),
                        parent_event_id UUID,
                        organizer_id UUID NOT NULL,
                        event_name VARCHAR(255) NOT NULL,
                        description TEXT,
                        location_id UUID,
                        event_date DATE NOT NULL,
                        event_time TIME NOT NULL,
                        currency_code VARCHAR(3),
                        price DECIMAL(10, 2),
                        max_attendees INTEGER,
                        allow_waitlist BOOLEAN NOT NULL DEFAULT false,
                        allow_partner_pairing BOOLEAN NOT NULL DEFAULT false,
                        promo_media_id UUID,
                        status VARCHAR(20) NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE events ADD CONSTRAINT pk_events PRIMARY KEY (id);


-- Media table
CREATE TABLE media (
                       id UUID NOT NULL DEFAULT gen_random_uuid(),
                       media_type VARCHAR(50) NOT NULL,
                       file_path TEXT NOT NULL,
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE media ADD CONSTRAINT pk_media PRIMARY KEY (id);

-- Event registration/attendees table
CREATE TABLE event_registration (
                                    id UUID NOT NULL DEFAULT gen_random_uuid(),
                                    event_id UUID NOT NULL,
                                    user_id UUID NOT NULL,
                                    role_id UUID,
                                    status VARCHAR(20) NOT NULL DEFAULT 'going',
                                    paid BOOLEAN NOT NULL DEFAULT false,
                                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE event_registration ADD CONSTRAINT pk_event_registration PRIMARY KEY (id);
ALTER TABLE event_registration ADD CONSTRAINT uc_event_registration_user_event UNIQUE (event_id, user_id);

-- Notifications table
CREATE TABLE notifications (
                               id UUID NOT NULL DEFAULT gen_random_uuid(),
                               user_id UUID NOT NULL,
                               notification_type VARCHAR(50) NOT NULL,
                               title VARCHAR(255) NOT NULL,
                               message TEXT,
                               is_read BOOLEAN NOT NULL DEFAULT false,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE notifications ADD CONSTRAINT pk_notifications PRIMARY KEY (id);

-- User media table
CREATE TABLE user_media (
                            user_id UUID NOT NULL,
                            media_id UUID NOT NULL,
                            is_primary BOOLEAN NOT NULL DEFAULT false,
                            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE user_media ADD CONSTRAINT pk_user_media PRIMARY KEY (user_id, media_id);

-- Junction table: dance_styles <-> events (many-to-many)
CREATE TABLE dance_styles_events (
                                     dance_style_id UUID NOT NULL,
                                     event_id UUID NOT NULL
);
ALTER TABLE dance_styles_events ADD CONSTRAINT pk_dance_styles_events PRIMARY KEY (dance_style_id, event_id);

-- Junction table: events <-> skill_levels (many-to-many)
CREATE TABLE events_skill_levels (
                                     event_id UUID NOT NULL,
                                     skill_level_id UUID NOT NULL
);
ALTER TABLE events_skill_levels ADD CONSTRAINT pk_events_skill_levels PRIMARY KEY (event_id, skill_level_id);

-- Junction table: user_profiles <-> dance_styles (tracks dance style interest)
CREATE TABLE user_dance_styles (
                                   user_id UUID NOT NULL,
                                   dance_style_id UUID NOT NULL,
                                   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE user_dance_styles ADD CONSTRAINT pk_user_dance_styles PRIMARY KEY (user_id, dance_style_id);

-- Junction table: events <-> event_types (many-to-many)
CREATE TABLE events_event_types (
                                    event_id UUID NOT NULL,
                                    event_type_id UUID NOT NULL
);
ALTER TABLE events_event_types ADD CONSTRAINT pk_events_event_types PRIMARY KEY (event_id, event_type_id);

-- events <-> media (many-to-many)
CREATE TABLE events_media (
                              event_id UUID NOT NULL,
                              media_id UUID NOT NULL,
                              display_order INTEGER DEFAULT 0
);
ALTER TABLE events_media ADD CONSTRAINT pk_events_media PRIMARY KEY (event_id, media_id);

-- Foreign Keys

-- user_profiles references
ALTER TABLE user_profiles ADD CONSTRAINT fk_user_profiles_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE user_profiles ADD CONSTRAINT fk_user_profiles_role FOREIGN KEY (role_id) REFERENCES dancer_role (id) ON DELETE SET NULL;
ALTER TABLE user_profiles ADD CONSTRAINT fk_user_profiles_general_skill FOREIGN KEY (general_skill_level_id) REFERENCES skill_levels (id) ON DELETE SET NULL;

-- events references
ALTER TABLE events ADD CONSTRAINT fk_events_organizer FOREIGN KEY (organizer_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE events ADD CONSTRAINT fk_events_parent FOREIGN KEY (parent_event_id) REFERENCES event_parent (id) ON DELETE SET NULL;
ALTER TABLE events ADD CONSTRAINT fk_events_currency FOREIGN KEY (currency_code) REFERENCES currencies (code) ON DELETE SET NULL;
ALTER TABLE events ADD CONSTRAINT fk_events_promo_media FOREIGN KEY (promo_media_id) REFERENCES media (id) ON DELETE SET NULL;
ALTER TABLE events ADD CONSTRAINT fk_events_location FOREIGN KEY (location_id) REFERENCES locations (id) ON DELETE SET NULL;

-- event_registration references
ALTER TABLE event_registration ADD CONSTRAINT fk_event_registration_events FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE;
ALTER TABLE event_registration ADD CONSTRAINT fk_event_registration_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE event_registration ADD CONSTRAINT fk_event_registration_role FOREIGN KEY (role_id) REFERENCES dancer_role (id) ON DELETE SET NULL;


-- notifications references
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

-- user_media references
ALTER TABLE user_media ADD CONSTRAINT fk_user_media_user_profiles FOREIGN KEY (user_id) REFERENCES user_profiles (user_id) ON DELETE CASCADE;
ALTER TABLE user_media ADD CONSTRAINT fk_user_media_media FOREIGN KEY (media_id) REFERENCES media (id) ON DELETE CASCADE;

-- Junction table foreign keys
ALTER TABLE dance_styles_events ADD CONSTRAINT fk_dance_styles_events_dance_styles FOREIGN KEY (dance_style_id) REFERENCES dance_styles (id) ON DELETE CASCADE;
ALTER TABLE dance_styles_events ADD CONSTRAINT fk_dance_styles_events_events FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE;

ALTER TABLE events_skill_levels ADD CONSTRAINT fk_events_skill_levels_events FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE;
ALTER TABLE events_skill_levels ADD CONSTRAINT fk_events_skill_levels_skill_levels FOREIGN KEY (skill_level_id) REFERENCES skill_levels (id) ON DELETE CASCADE;

ALTER TABLE user_dance_styles ADD CONSTRAINT fk_user_dance_styles_users FOREIGN KEY (user_id) REFERENCES user_profiles (user_id) ON DELETE CASCADE;
ALTER TABLE user_dance_styles ADD CONSTRAINT fk_user_dance_styles_dance_styles FOREIGN KEY (dance_style_id) REFERENCES dance_styles (id) ON DELETE CASCADE;

ALTER TABLE events_media ADD CONSTRAINT fk_events_media_events FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE;
ALTER TABLE events_media ADD CONSTRAINT fk_events_media_media FOREIGN KEY (media_id) REFERENCES media (id) ON DELETE CASCADE;

-- Indexes for performance
CREATE INDEX idx_events_organizer ON events(organizer_id);
CREATE INDEX idx_events_date ON events(event_date);
CREATE INDEX idx_events_parent ON events(parent_event_id);
CREATE INDEX idx_events_location ON events(location_id);
CREATE INDEX idx_event_registration_event ON event_registration(event_id);
CREATE INDEX idx_event_registration_user ON event_registration(user_id);
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);
CREATE INDEX idx_user_dance_styles_user ON user_dance_styles(user_id);
CREATE INDEX idx_user_dance_styles_dance ON user_dance_styles(dance_style_id);
CREATE INDEX idx_user_media_user ON user_media(user_id);
CREATE INDEX idx_user_media_primary ON user_media(user_id, is_primary);

-- =====================================================
-- SEED DATA - Predefined Values
-- =====================================================

-- Insert predefined skill levels (order: 0=Beginner, 1=Intermediate, 2=Advanced, 3=Professional)
INSERT INTO skill_levels (id, name, level_order) VALUES
                                                     (gen_random_uuid(), 'Beginner', 0),
                                                     (gen_random_uuid(), 'Intermediate', 1),
                                                     (gen_random_uuid(), 'Advanced', 2),
                                                     (gen_random_uuid(), 'Professional', 3);

-- Insert predefined dance styles
INSERT INTO dance_styles (id, name) VALUES
                                        (gen_random_uuid(), 'Bachata'),
                                        (gen_random_uuid(), 'Salsa'),
                                        (gen_random_uuid(), 'Kizomba'),
                                        (gen_random_uuid(), 'Zouk'),
                                        (gen_random_uuid(), 'Tango'),
                                        (gen_random_uuid(), 'West Coast Swing'),
                                        (gen_random_uuid(), 'Lindy Hop'),
                                        (gen_random_uuid(), 'Forró'),
                                        (gen_random_uuid(), 'Merengue'),
                                        (gen_random_uuid(), 'Cha-cha');

-- Insert predefined currencies
INSERT INTO currencies (code, name, symbol) VALUES
                                                ('USD', 'US Dollar', '$'),
                                                ('EUR', 'Euro', '€'),
                                                ('CZK', 'Czech Koruna', 'Kč'),
                                                ('GBP', 'British Pound', '£');

-- Insert predefined event types
INSERT INTO event_type (name) VALUES
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

-- Insert predefined roles (Leader/Follower/Both)
INSERT INTO dancer_role (name) VALUES
                                   ('Leader'),
                                   ('Follower'),
                                   ('Both');

-- =====================================================
-- SAMPLE DATA GENERATION
-- =====================================================
DO $$
DECLARE
    -- IDs for lookups
v_role_leader UUID;
    v_role_follower UUID;
    v_level_inter UUID;
    v_style_salsa UUID;
    v_style_bachata UUID;
    v_type_party UUID;
    v_type_class UUID;

    -- User IDs
    v_user_alice UUID;
    v_user_bob UUID;
    v_user_charlie UUID;

    -- Event IDs
    v_event_party UUID;
    v_event_parent UUID;
    v_event_child UUID;

    -- Location IDs
    v_loc_river_bar UUID;
    v_loc_studio UUID;

BEGIN
    -- 1. Fetch Lookup IDs
SELECT id INTO v_role_leader FROM dancer_role WHERE name = 'Leader' LIMIT 1;
SELECT id INTO v_role_follower FROM dancer_role WHERE name = 'Follower' LIMIT 1;
SELECT id INTO v_level_inter FROM skill_levels WHERE name = 'Intermediate' LIMIT 1;
SELECT id INTO v_style_salsa FROM dance_styles WHERE name = 'Salsa' LIMIT 1;
SELECT id INTO v_style_bachata FROM dance_styles WHERE name = 'Bachata' LIMIT 1;
SELECT id INTO v_type_party FROM event_type WHERE name = 'Party' LIMIT 1;
SELECT id INTO v_type_class FROM event_type WHERE name = 'Class' LIMIT 1;

-- 2. Create Users
INSERT INTO users (email, provider, provider_id) VALUES
    ('alice@example.com', 'google', 'google_alice_123') RETURNING id INTO v_user_alice;
INSERT INTO users (email, provider, provider_id) VALUES
    ('bob@example.com', 'google', 'google_bob_456') RETURNING id INTO v_user_bob;
INSERT INTO users (email, provider, provider_id) VALUES
    ('charlie@example.com', 'google', 'google_charlie_789') RETURNING id INTO v_user_charlie;

-- 3. Create User Profiles
INSERT INTO user_profiles (user_id, first_name, last_name, bio, role_id, general_skill_level_id, city, country) VALUES
                                                                                                                    (v_user_alice, 'Alice', 'Wonder', 'Loves Salsa and organizing events.', v_role_follower, v_level_inter, 'Prague', 'Czech Republic'),
                                                                                                                    (v_user_bob, 'Bob', 'Builder', 'Bachata enthusiast.', v_role_leader, v_level_inter, 'Prague', 'Czech Republic'),
                                                                                                                    (v_user_charlie, 'Charlie', 'Chaplin', 'Just starting out.', v_role_leader, NULL, 'Brno', 'Czech Republic');

-- 4. Create Locations
INSERT INTO locations (name, street, house_number, city, country) VALUES
    ('River Bar', 'River St', '1', 'Prague', 'Czech Republic') RETURNING id INTO v_loc_river_bar;
INSERT INTO locations (name, street, house_number, city, country) VALUES
    ('Dance Studio 1', 'Main St', '10', 'Prague', 'Czech Republic') RETURNING id INTO v_loc_studio;

-- 5. Create Individual Event (Bachata Party)
INSERT INTO events (
    organizer_id, event_name, description, location_id,
    event_date, event_time, status
) VALUES (
             v_user_alice, 'Summer Bachata Night', 'Open air dancing by the river.', v_loc_river_bar,
             CURRENT_DATE + INTERVAL '5 days', '20:00', 'PUBLISHED'
         ) RETURNING id INTO v_event_party;

-- Link styles and types
INSERT INTO dance_styles_events (dance_style_id, event_id) VALUES (v_style_bachata, v_event_party);
INSERT INTO events_event_types (event_id, event_type_id) VALUES (v_event_party, v_type_party);

-- 6. Create Recurring Parent Event (Salsa Class)
INSERT INTO event_parent (name) VALUES ('Weekly Salsa Foundations') RETURNING id INTO v_event_parent;

-- 7. Create Occurrences
FOR i IN 0..3 LOOP
        INSERT INTO events (
            parent_event_id, organizer_id, event_name, description, location_id,
            event_date, event_time, status
        ) VALUES (
            v_event_parent, v_user_alice, 'Weekly Salsa Foundations (Week ' || (i+1) || ')', 'Learn the basics every Tuesday.', v_loc_studio,
            CURRENT_DATE + INTERVAL '2 days' + (i * 7 || ' days')::INTERVAL, '18:00', 'PUBLISHED'
        ) RETURNING id INTO v_event_child;

INSERT INTO dance_styles_events (dance_style_id, event_id) VALUES (v_style_salsa, v_event_child);
INSERT INTO events_event_types (event_id, event_type_id) VALUES (v_event_child, v_type_class);
END LOOP;

    -- 8. Add Registrations
    -- Bob goes to the party
INSERT INTO event_registration (event_id, user_id, role_id, status) VALUES (v_event_party, v_user_bob, v_role_leader, 'going');
-- Charlie is interested in the party
INSERT INTO event_registration (event_id, user_id, role_id, status) VALUES (v_event_party, v_user_charlie, v_role_leader, 'interested');

END $$;