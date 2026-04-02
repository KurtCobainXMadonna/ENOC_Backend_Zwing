-- =============================================================================
-- V2: Sound library + Channel Rack + Channels
-- =============================================================================

-- 1. Sound presets (the library of available sounds)
CREATE TABLE sound_presets (
    sound_id    UUID PRIMARY KEY,
    name        VARCHAR(255)  NOT NULL,
    category    VARCHAR(50)   NOT NULL,          -- KICK, SNARE, HIHAT, CLAP, SYNTH, SAMPLE
    blob_url    VARCHAR(1024) NOT NULL,          -- Azure Blob Storage / CDN URL
    description TEXT
);

-- 2. Channel racks (one per project)
--    Note: no FK from project_id → projects because the rack is created
--    BEFORE the project is saved (in ProjectService.createProject).
--    The reverse FK (projects.channel_rack_id → channel_racks.rack_id) enforces the relationship.
CREATE TABLE channel_racks (
    rack_id    UUID PRIMARY KEY,
    project_id UUID NOT NULL UNIQUE,
    bpm        INT  NOT NULL DEFAULT 120
);

-- 3. Channels (belong to a rack, reference a sound preset)
CREATE TABLE channels (
    channel_id UUID PRIMARY KEY,
    rack_id    UUID         NOT NULL,
    name       VARCHAR(255) NOT NULL,
    sound_id   UUID         NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    volume     REAL         NOT NULL DEFAULT 1.0,
    steps      VARCHAR(127) NOT NULL,            -- "false,false,...,false" (16 comma-separated booleans)
    position   INT          NOT NULL,
    CONSTRAINT fk_channel_rack  FOREIGN KEY (rack_id)  REFERENCES channel_racks (rack_id) ON DELETE CASCADE,
    CONSTRAINT fk_channel_sound FOREIGN KEY (sound_id) REFERENCES sound_presets (sound_id)
);

-- 4. Add rack reference column to projects
ALTER TABLE projects ADD COLUMN channel_rack_id UUID;
ALTER TABLE projects ADD CONSTRAINT fk_project_rack FOREIGN KEY (channel_rack_id) REFERENCES channel_racks (rack_id) ON DELETE SET NULL;

-- =============================================================================
-- 5. Seed the default sound library (8 presets: 2 per category)
--    Replace blob_url values with your actual Azure Blob Storage URLs.
-- =============================================================================
INSERT INTO sound_presets (sound_id, name, category, blob_url, description) VALUES
                                                                                ('a1000000-0000-0000-0000-000000000001', 'Kick Classic',  'KICK',  'https://YOUR_STORAGE.blob.core.windows.net/sounds/kick-classic.wav',  'Punchy classic kick'),
                                                                                ('a1000000-0000-0000-0000-000000000002', 'Kick Deep',     'KICK',  'https://YOUR_STORAGE.blob.core.windows.net/sounds/kick-deep.wav',     'Deep sub kick'),
                                                                                ('a2000000-0000-0000-0000-000000000001', 'Snare Crack',   'SNARE', 'https://YOUR_STORAGE.blob.core.windows.net/sounds/snare-crack.wav',   'Sharp crack snare'),
                                                                                ('a2000000-0000-0000-0000-000000000002', 'Snare Fat',     'SNARE', 'https://YOUR_STORAGE.blob.core.windows.net/sounds/snare-fat.wav',     'Fat layered snare'),
                                                                                ('a3000000-0000-0000-0000-000000000001', 'Hi-Hat Closed', 'HIHAT', 'https://YOUR_STORAGE.blob.core.windows.net/sounds/hihat-closed.wav',  'Tight closed hi-hat'),
                                                                                ('a3000000-0000-0000-0000-000000000002', 'Hi-Hat Open',   'HIHAT', 'https://YOUR_STORAGE.blob.core.windows.net/sounds/hihat-open.wav',    'Washy open hi-hat'),
                                                                                ('a4000000-0000-0000-0000-000000000001', 'Clap Standard', 'CLAP',  'https://YOUR_STORAGE.blob.core.windows.net/sounds/clap-standard.wav', 'Classic 808 clap'),
                                                                                ('a4000000-0000-0000-0000-000000000002', 'Clap Reverb',   'CLAP',  'https://YOUR_STORAGE.blob.core.windows.net/sounds/clap-reverb.wav',   'Room reverb clap');
