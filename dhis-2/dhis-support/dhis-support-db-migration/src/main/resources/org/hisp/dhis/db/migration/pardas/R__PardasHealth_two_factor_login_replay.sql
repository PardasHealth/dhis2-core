-- Persistent login replay reservations shared by all application nodes.
-- No raw OTP, seed, or user identifier is stored in this table.
create table if not exists usedtwofactorcode (
    codehash varchar(64) not null primary key,
    expires bigint not null
);
create index if not exists usedtwofactorcode_expires on usedtwofactorcode (expires);
