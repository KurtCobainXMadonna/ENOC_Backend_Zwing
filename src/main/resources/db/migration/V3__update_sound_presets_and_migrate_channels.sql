-- =============================================================================
-- V3: Migration to new Sound Library URLs and Preservation of Existing Projects
-- =============================================================================

-- 1. Insert the 8 new valid sound presets with fresh IDs
-- Using %20 for spaces to ensure URL compatibility
INSERT INTO sound_presets (sound_id, name, category, blob_url, description) VALUES
    ('b1000000-0000-0000-0000-000000000001', 'Basic 808 Kick',  'KICK',  'https://zwingaudiostorage.blob.core.windows.net/audio-presets/Basic%20808%20Kick.wav',  'Classic 808 style kick drum'),
    ('b1000000-0000-0000-0000-000000000002', 'Basic 808 Snare', 'SNARE', 'https://zwingaudiostorage.blob.core.windows.net/audio-presets/Basic%20808%20Snare.wav', 'Classic 808 style snare drum'),
    ('b1000000-0000-0000-0000-000000000003', 'Basic 808 Clap',  'CLAP',  'https://zwingaudiostorage.blob.core.windows.net/audio-presets/Basic%20808%20Clap.wav',  'Classic 808 style clap'),
    ('b1000000-0000-0000-0000-000000000004', 'Basic 808 HiHat', 'HIHAT', 'https://zwingaudiostorage.blob.core.windows.net/audio-presets/Basic%20808%20HiHat.wav', 'Classic 808 style closed hi-hat'),
    ('b2000000-0000-0000-0000-000000000001', 'CB Kick',         'KICK',  'https://zwingaudiostorage.blob.core.windows.net/audio-presets/CB_Kick.wav',         'Punchy CB series kick'),
    ('b2000000-0000-0000-0000-000000000002', 'CB Snare',        'SNARE', 'https://zwingaudiostorage.blob.core.windows.net/audio-presets/CB_Snare.wav',        'Crisp CB series snare'),
    ('b2000000-0000-0000-0000-000000000003', 'CB Clap',         'CLAP',  'https://zwingaudiostorage.blob.core.windows.net/audio-presets/CB_Clap.wav',         'Clean CB series clap'),
    ('b2000000-0000-0000-0000-000000000004', 'CB Hat',          'HIHAT', 'https://zwingaudiostorage.blob.core.windows.net/audio-presets/CB_Hat.wav',          'Sharp CB series hi-hat');

-- 2. Update existing channels to point to the new IDs so we don't break foreign keys
-- We map the old "a" IDs to the closest "b" IDs

-- KICKS: Hardstyle & Bass Line -> 808 Kick & CB Kick
UPDATE channels SET sound_id = 'b1000000-0000-0000-0000-000000000001' WHERE sound_id = 'a1000000-0000-0000-0000-000000000001';
UPDATE channels SET sound_id = 'b2000000-0000-0000-0000-000000000001' WHERE sound_id = 'a1000000-0000-0000-0000-000000000002';

-- SNARES: Power Snare & Guitar Loop -> 808 Snare & CB Snare
UPDATE channels SET sound_id = 'b1000000-0000-0000-0000-000000000002' WHERE sound_id = 'a2000000-0000-0000-0000-000000000001';
UPDATE channels SET sound_id = 'b2000000-0000-0000-0000-000000000002' WHERE sound_id = 'a2000000-0000-0000-0000-000000000002';

-- SAMPLES: Sad Piano -> CB Kick (Temporary fallback since there's no new Sample category yet)
UPDATE channels SET sound_id = 'b2000000-0000-0000-0000-000000000001' WHERE sound_id = 'a5000000-0000-0000-0000-000000000001';

-- HIHATS & CLAPS (Handling the commented-out IDs from V2 just in case they exist)
UPDATE channels SET sound_id = 'b1000000-0000-0000-0000-000000000004' WHERE sound_id::text LIKE 'a3000%';
UPDATE channels SET sound_id = 'b1000000-0000-0000-0000-000000000003' WHERE sound_id::text LIKE 'a4000%';

-- 3. Final Cleanup: Delete the old records that no longer have any references
DELETE FROM sound_presets WHERE sound_id::text LIKE 'a%';