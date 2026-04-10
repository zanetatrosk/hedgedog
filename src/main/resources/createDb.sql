-- Schema-only script
-- Creates all tables, constraints, and indexes.

-- Remove conflicting tables
DROP TABLE IF EXISTS dance_styles CASCADE;
DROP TABLE IF EXISTS media CASCADE;
DROP TABLE IF EXISTS registrations CASCADE;
DROP TABLE IF EXISTS event_registration CASCADE;
DROP TABLE IF EXISTS event_registration_settings CASCADE;
DROP TABLE IF EXISTS event_types CASCADE;
DROP TABLE IF EXISTS events CASCADE;
DROP TABLE IF EXISTS dancer_roles CASCADE;
DROP TABLE IF EXISTS skill_levels CASCADE;
DROP TABLE IF EXISTS user_profiles CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS user_favorites CASCADE;
DROP TABLE IF EXISTS dance_styles_events CASCADE;
DROP TABLE IF EXISTS events_skill_levels CASCADE;
DROP TABLE IF EXISTS user_dance_interests CASCADE;
DROP TABLE IF EXISTS user_dance_styles CASCADE;
DROP TABLE IF EXISTS events_event_types CASCADE;
DROP TABLE IF EXISTS currencies CASCADE;
DROP TABLE IF EXISTS event_parents CASCADE;
DROP TABLE IF EXISTS events_media CASCADE;
DROP TABLE IF EXISTS user_media CASCADE;
DROP TABLE IF EXISTS profile_media CASCADE;
DROP TABLE IF EXISTS event_media CASCADE;
DROP TABLE IF EXISTS locations CASCADE;

-- Users table
CREATE TABLE users (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    google_access_token VARCHAR(2048),
    google_refresh_token VARCHAR(512),
    google_token_expiry TIMESTAMP WITH TIME ZONE,
    google_scopes VARCHAR(1024),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP WITH TIME ZONE
);
ALTER TABLE users ADD CONSTRAINT pk_users PRIMARY KEY (id);

CREATE TABLE user_profiles (
    user_id UUID NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    bio TEXT,
    role_id UUID,
    general_skill_level_id UUID,
    avatar_media_id UUID,
    city VARCHAR(100),
    country VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE user_profiles ADD CONSTRAINT pk_user_profiles PRIMARY KEY (user_id);

CREATE TABLE dancer_roles (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL
);
ALTER TABLE dancer_roles ADD CONSTRAINT pk_dancer_roles PRIMARY KEY (id);

CREATE TABLE dance_styles (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE dance_styles ADD CONSTRAINT pk_dance_styles PRIMARY KEY (id);
ALTER TABLE dance_styles ADD CONSTRAINT uc_dance_styles_name UNIQUE (name);

CREATE TABLE currencies (
    code VARCHAR(3) NOT NULL,
    name VARCHAR(50) NOT NULL,
    symbol VARCHAR(10),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE currencies ADD CONSTRAINT pk_currencies PRIMARY KEY (code);

CREATE TABLE skill_levels (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL,
    level_order INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE skill_levels ADD CONSTRAINT pk_skill_levels PRIMARY KEY (id);
ALTER TABLE skill_levels ADD CONSTRAINT uc_skill_levels_name UNIQUE (name);
ALTER TABLE skill_levels ADD CONSTRAINT uc_skill_levels_order UNIQUE (level_order);

CREATE TABLE event_types (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE event_types ADD CONSTRAINT pk_event_types PRIMARY KEY (id);
ALTER TABLE event_types ADD CONSTRAINT uc_event_types_name UNIQUE (name);

CREATE TABLE event_parents (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL
);
ALTER TABLE event_parents ADD CONSTRAINT pk_event_parents PRIMARY KEY (id);

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

CREATE TABLE events (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    parent_event_id UUID,
    organizer_id UUID NOT NULL,
    event_name VARCHAR(255) NOT NULL,
    description TEXT,
    location_id UUID,
    event_date DATE NOT NULL,
    event_time TIME NOT NULL,
    end_date DATE,
    currency_code VARCHAR(3),
    price DECIMAL(10, 2),
    max_attendees INTEGER,
    promo_media_id UUID,
    status VARCHAR(20) NOT NULL,
    facebook_event_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE events ADD CONSTRAINT pk_events PRIMARY KEY (id);

CREATE TABLE event_registration_settings (
    event_id UUID PRIMARY KEY REFERENCES events(id) ON DELETE CASCADE,
    registration_mode VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    form_id VARCHAR(255),
    form_structure JSONB,
    allow_waitlist BOOLEAN NOT NULL DEFAULT false,
    require_approval BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE media (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    media_type VARCHAR(50) NOT NULL,
    file_path TEXT NOT NULL,
    owner_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE media ADD CONSTRAINT pk_media PRIMARY KEY (id);

CREATE TABLE registrations (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    user_id UUID,
    role_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'registered',
    email VARCHAR(255) NOT NULL,
    is_anonymous BOOLEAN NOT NULL DEFAULT false,
    response_id varchar(255),
    form_responses JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    waitlisted_at TIMESTAMP WITH TIME ZONE
);
ALTER TABLE registrations ADD CONSTRAINT pk_registrations PRIMARY KEY (id);
ALTER TABLE registrations ADD CONSTRAINT uc_registrations_user_event UNIQUE (event_id, user_id);

CREATE TABLE user_media (
    user_id UUID NOT NULL,
    media_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE user_media ADD CONSTRAINT pk_user_media PRIMARY KEY (user_id, media_id);

CREATE TABLE dance_styles_events (
    dance_style_id UUID NOT NULL,
    event_id UUID NOT NULL
);
ALTER TABLE dance_styles_events ADD CONSTRAINT pk_dance_styles_events PRIMARY KEY (dance_style_id, event_id);

CREATE TABLE events_skill_levels (
    event_id UUID NOT NULL,
    skill_level_id UUID NOT NULL
);
ALTER TABLE events_skill_levels ADD CONSTRAINT pk_events_skill_levels PRIMARY KEY (event_id, skill_level_id);

CREATE TABLE user_dance_styles (
    user_id UUID NOT NULL,
    dance_style_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE user_dance_styles ADD CONSTRAINT pk_user_dance_styles PRIMARY KEY (user_id, dance_style_id);

CREATE TABLE events_event_types (
    event_id UUID NOT NULL,
    event_type_id UUID NOT NULL
);
ALTER TABLE events_event_types ADD CONSTRAINT pk_events_event_types PRIMARY KEY (event_id, event_type_id);

CREATE TABLE events_media (
    event_id UUID NOT NULL,
    media_id UUID NOT NULL,
    display_order INTEGER DEFAULT 0
);
ALTER TABLE events_media ADD CONSTRAINT pk_events_media PRIMARY KEY (event_id, media_id);

-- Foreign Keys
ALTER TABLE user_profiles ADD CONSTRAINT fk_user_profiles_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE user_profiles ADD CONSTRAINT fk_user_profiles_role FOREIGN KEY (role_id) REFERENCES dancer_roles (id) ON DELETE SET NULL;
ALTER TABLE user_profiles ADD CONSTRAINT fk_user_profiles_general_skill FOREIGN KEY (general_skill_level_id) REFERENCES skill_levels (id) ON DELETE SET NULL;
ALTER TABLE user_profiles ADD CONSTRAINT fk_user_profiles_avatar_media FOREIGN KEY (avatar_media_id) REFERENCES media (id) ON DELETE SET NULL;

ALTER TABLE media ADD CONSTRAINT fk_media_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE events ADD CONSTRAINT fk_events_organizer FOREIGN KEY (organizer_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE events ADD CONSTRAINT fk_events_parent FOREIGN KEY (parent_event_id) REFERENCES event_parents (id) ON DELETE SET NULL;
ALTER TABLE events ADD CONSTRAINT fk_events_currency FOREIGN KEY (currency_code) REFERENCES currencies (code) ON DELETE SET NULL;
ALTER TABLE events ADD CONSTRAINT fk_events_promo_media FOREIGN KEY (promo_media_id) REFERENCES media (id) ON DELETE SET NULL;
ALTER TABLE events ADD CONSTRAINT fk_events_location FOREIGN KEY (location_id) REFERENCES locations (id) ON DELETE SET NULL;

ALTER TABLE registrations ADD CONSTRAINT fk_registrations_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE;
ALTER TABLE registrations ADD CONSTRAINT fk_registrations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE registrations ADD CONSTRAINT fk_registrations_role FOREIGN KEY (role_id) REFERENCES dancer_roles (id) ON DELETE SET NULL;

ALTER TABLE user_media ADD CONSTRAINT fk_user_media_user_profiles FOREIGN KEY (user_id) REFERENCES user_profiles (user_id) ON DELETE CASCADE;
ALTER TABLE user_media ADD CONSTRAINT fk_user_media_media FOREIGN KEY (media_id) REFERENCES media (id) ON DELETE CASCADE;

ALTER TABLE dance_styles_events ADD CONSTRAINT fk_dance_styles_events_dance_styles FOREIGN KEY (dance_style_id) REFERENCES dance_styles (id) ON DELETE CASCADE;
ALTER TABLE dance_styles_events ADD CONSTRAINT fk_dance_styles_events_events FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE;

ALTER TABLE events_skill_levels ADD CONSTRAINT fk_events_skill_levels_events FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE;
ALTER TABLE events_skill_levels ADD CONSTRAINT fk_events_skill_levels_skill_levels FOREIGN KEY (skill_level_id) REFERENCES skill_levels (id) ON DELETE CASCADE;

ALTER TABLE user_dance_styles ADD CONSTRAINT fk_user_dance_styles_users FOREIGN KEY (user_id) REFERENCES user_profiles (user_id) ON DELETE CASCADE;
ALTER TABLE user_dance_styles ADD CONSTRAINT fk_user_dance_styles_dance_styles FOREIGN KEY (dance_style_id) REFERENCES dance_styles (id) ON DELETE CASCADE;

ALTER TABLE events_event_types ADD CONSTRAINT fk_events_event_types_events FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE;
ALTER TABLE events_event_types ADD CONSTRAINT fk_events_event_types_event_types FOREIGN KEY (event_type_id) REFERENCES event_types (id) ON DELETE CASCADE;

ALTER TABLE events_media ADD CONSTRAINT fk_events_media_events FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE;
ALTER TABLE events_media ADD CONSTRAINT fk_events_media_media FOREIGN KEY (media_id) REFERENCES media (id) ON DELETE CASCADE;

-- Indexes
CREATE INDEX idx_events_organizer ON events(organizer_id);
CREATE INDEX idx_events_date ON events(event_date);
CREATE INDEX idx_events_parent ON events(parent_event_id);
CREATE INDEX idx_events_location ON events(location_id);
CREATE INDEX idx_registrations_event ON registrations(event_id);
CREATE INDEX idx_registrations_user ON registrations(user_id);
CREATE INDEX idx_user_dance_styles_user ON user_dance_styles(user_id);
CREATE INDEX idx_user_dance_styles_dance ON user_dance_styles(dance_style_id);
CREATE INDEX idx_user_media_user ON user_media(user_id);
CREATE INDEX idx_media_owner ON media(owner_id);
CREATE INDEX idx_registrations_event_waitlisted_at ON registrations(event_id, waitlisted_at);

