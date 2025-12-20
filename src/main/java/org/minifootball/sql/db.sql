-- créer la base de donnée
CREATE DATABASE mini_football_db;

-- créer l'utilisateur
CREATE USER mini_football_db_manager WITH PASSWORD 'jese';

-- privilège à l'utilisateur sur la base de donnée
GRANT CONNECT ON mini_football_db TO mini_football_db_manager;

-- privilège à l'utilisateur de faire CRUD sur les tables
GRANT INSERT , DELETE , SELECT , UPDATE ON ALL TABLES IN CHEMA PUBILC TO mini_football_db_mmanager;