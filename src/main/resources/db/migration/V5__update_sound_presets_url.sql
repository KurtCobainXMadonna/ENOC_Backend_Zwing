-- Actualización de URLs para los presets de la serie 808
UPDATE sound_presets
SET blob_url = 'https://zwingblob32125.blob.core.windows.net/audio-presets/Basic 808 Kick.wav'
WHERE sound_id = 'b1000000-0000-0000-0000-000000000001';

UPDATE sound_presets
SET blob_url = 'https://zwingblob32125.blob.core.windows.net/audio-presets/Basic 808 Snare.wav'
WHERE sound_id = 'b1000000-0000-0000-0000-000000000002';

UPDATE sound_presets
SET blob_url = 'https://zwingblob32125.blob.core.windows.net/audio-presets/Basic 808 Clap.wav'
WHERE sound_id = 'b1000000-0000-0000-0000-000000000003';

UPDATE sound_presets
SET blob_url = 'https://zwingblob32125.blob.core.windows.net/audio-presets/Basic 808 HiHat.wav'
WHERE sound_id = 'b1000000-0000-0000-0000-000000000004';

-- Actualización de URLs para los presets de la serie CB
UPDATE sound_presets
SET blob_url = 'https://zwingblob32125.blob.core.windows.net/audio-presets/CB_Kick.wav'
WHERE sound_id = 'b2000000-0000-0000-0000-000000000001';

UPDATE sound_presets
SET blob_url = 'https://zwingblob32125.blob.core.windows.net/audio-presets/CB_Snare.wav'
WHERE sound_id = 'b2000000-0000-0000-0000-000000000002';

UPDATE sound_presets
SET blob_url = 'https://zwingblob32125.blob.core.windows.net/audio-presets/CB_Clap.wav'
WHERE sound_id = 'b2000000-0000-0000-0000-000000000003';

UPDATE sound_presets
SET blob_url = 'https://zwingblob32125.blob.core.windows.net/audio-presets/CB_Hat.wav'
WHERE sound_id = 'b2000000-0000-0000-0000-000000000004';