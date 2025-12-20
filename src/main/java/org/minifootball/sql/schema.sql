-- créer enum des continents
CREATE TYPE continent_enum AS ENUM ('AFRICA', 'EUROPA' , 'ASIA' , 'AMERICA');

-- créer enum des positions des joueurs
CREATE TYPE player_position AS ENUM ('GK', 'DEF' , 'MIDF', 'STR');

-- créer la table Team
CREATE TABLE Team (
    id INT PRIMARY KEY ,
    name VARCHAR NOT NULL ,
    continent continent_enum NOT NULL
);

-- créer la table Player
CREATE TABLE Player (
    id INT PRIMARY KEY ,
    name VARCHAR NOT NULL ,
    age INT NOT NULL,
    position player_position NOT NULL ,
    id_team INT ,
        CONSTRAINT fk_team
            FOREIGN KEY (id_team) REFERENCES Team(id)
                    ON DELETE SET NULL
);