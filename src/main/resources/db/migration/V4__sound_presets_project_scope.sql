-- =============================================================================
-- V4: Project-scoped sound uploads
-- Existing rows (NULL project_id) remain visible to all projects (global presets).
-- Rows with a project_id are user uploads visible only to that project's members.
-- =============================================================================

ALTER TABLE sound_presets
    ADD COLUMN project_id  UUID NULL,
    ADD COLUMN uploaded_by UUID NULL;

ALTER TABLE sound_presets
    ADD CONSTRAINT fk_sound_presets_project
        FOREIGN KEY (project_id) REFERENCES projects (project_id)
        ON DELETE CASCADE;

CREATE INDEX idx_sound_presets_project_id ON sound_presets (project_id);
