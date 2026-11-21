-- Target: catalog_db only. customer_id is an external Identity ID, so only the Movie FK stays (AGENTS section 19.2).
CREATE TABLE dbo.Watchlist (
    watchlist_id BIGINT IDENTITY(1,1) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    movie_id VARCHAR(36) COLLATE Latin1_General_100_BIN2 NOT NULL,
    added_at DATETIME2 NOT NULL CONSTRAINT DF_Watchlist_added_at DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_Watchlist PRIMARY KEY (watchlist_id),
    CONSTRAINT UQ_Watchlist_customer_movie UNIQUE (customer_id,movie_id),
    CONSTRAINT FK_Watchlist_Movie FOREIGN KEY (movie_id) REFERENCES dbo.Movie(movie_id)
);
