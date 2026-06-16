-- Create main application database
CREATE DATABASE IF NOT EXISTS achievement_db
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Create classified data schema (separate schema for classified achievement data per D-39)
CREATE DATABASE IF NOT EXISTS achievement_classified
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
