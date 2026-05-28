posts
USE meerkatgrammeerkatgramusers;

CREATE TABLE users

(
     id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT 
    ,email         VARCHAR(100) NOT NULL UNIQUE KEY 
    ,password      VARCHAR(255) NOT NULL
    ,nick          VARCHAR(20)  NOT NULL
    ,provider      VARCHAR(10)  NOT NULL DEFAULT 'NONE'
    ,role          VARCHAR(10)  NOT NULL DEFAULT 'NORMAL'
    ,profile       VARCHAR(100) NOT NULL
    ,refresh_token VARCHAR(255) NULL	DEFAULT NULL
    ,created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP()
    ,updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP()
    ,deleted_at    DATETIME	NULL DEFAULT NULL
);