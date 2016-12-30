create table test9 (
    id int,
    name varchar(10),
    constraint test9_pk primary key (id));

create table test8_9 (
    id int,
    id9 int,
    name varchar(10),
    constraint id1_fk foreign key (id9) references test9(id),
    constraint test8_pk primary key (id));

create table test3_8 (
    id int,
    id8 int,
    name varchar(10),
    constraint test3_2_id8_fk foreign key (id8) references test8_9(id),
    constraint test3_2_pk primary key (id));

create table test4_9 (
    id int,
    id9 int,
    name varchar(10),
    constraint test4_1_id1_fk foreign key (id9) references test9(id),
    constraint test4_1_pk primary key (id));

alter table test9 add column id4 int;
alter table test9 add constraint test9_id4_fk foreign key (id4) references test4_9(id);