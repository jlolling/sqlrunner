create table dwh_ods.blob_test (
    id integer not null,
    bin_data blob,
    constraint pk_blob_test primary key (id))
    
 select * from dwh_ods.blob_test