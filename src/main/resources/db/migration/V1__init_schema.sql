-- 1. Create the Users table
CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);

-- 2. Create the Projects table
CREATE TABLE projects (
    project_id UUID PRIMARY KEY,
    project_name VARCHAR(255) NOT NULL,
    owner_id UUID NOT NULL,
    CONSTRAINT fk_project_owner FOREIGN KEY (owner_id) REFERENCES users (user_id)
);

-- 3. Create the Many-To-Many Join Table for Collaborators
CREATE TABLE project_collaborators (
    project_id UUID NOT NULL,
    user_id UUID NOT NULL,
    PRIMARY KEY (project_id, user_id),
    CONSTRAINT fk_pc_project FOREIGN KEY (project_id) REFERENCES projects (project_id),
    CONSTRAINT fk_pc_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);