CREATE TABLE conversations (
                               id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               customer_name VARCHAR(100) NOT NULL,
                               status        VARCHAR(20)  NOT NULL,
                               created_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE messages (
                          id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          conversation_id BIGINT       NOT NULL REFERENCES conversations(id),
                          sender          VARCHAR(100) NOT NULL,
                          content         VARCHAR(2000) NOT NULL,
                          sent_at         TIMESTAMPTZ  NOT NULL
);
