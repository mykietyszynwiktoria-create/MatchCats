create table subscriptions (
        subscriptionsID int primary key,
        startdatetime timestamp not null,
        enddatetime timestamp not null
);
INSERT into subscriptions (subscriptionsID, startdatetime, enddatetime)
VALUES(1, '2016-06-22 19:10:25-07', '2017-07-21 20:10:21-04');
INSERT into subscriptions (subscriptionsID, startdatetime, enddatetime)
VALUES(2, '2018-08-22 19:10:25-07', '2019-07-21 21:10:21-04');

create table usercat (
        usercatID int primary key,
        freesubscriptions int not null,
        counterforfreesub int not null,
        name1 varchar(50) not null

        );
insert into usercat values (1, 2, 3, 1);
insert into usercat values (2, 3, 2, 3);

create table cat (
        catID int primary key,
        catname varchar(20),
        catcolor varchar(20),
        userID int,

        CONSTRAINT fk_userID_usercat
   FOREIGN KEY(userID)
   REFERENCES usercat(usercatID)
        );
insert into cat values (1, 'Puszek', 'bialy', 1);
insert into cat values (2, 'Puszek', 'rudy', 2);
insert into cat values (3, 'Puszek', 'bialy', 1);
insert into cat values (4, 'Celina', 'niebieski', 3);
insert into cat values (5, 'Celina', 'niebieski', 4);
insert into cat values (6, 'Pysio', 'rudy', 2);

create table chat_information (
        chat_informationID int primary key,
        chat_cats varchar(50) not null,
        user1ID int not null,
        user2ID int not null,


        CONSTRAINT fk_user1ID_cat
   FOREIGN KEY(user1ID)
   REFERENCES cat(catID),
   CONSTRAINT fk_user2ID_cat
   FOREIGN KEY(user2ID)
   REFERENCES cat(catID)
        );
insert into chat_information values (1,'Para', 3, 1);
insert into chat_information values (1,'Para', 4, 5);

create table relationshipcats (
        relationshipcatsID int primary key,
        firstcatID int ,
        secondcatID int,
        possiblechatID int,

        CONSTRAINT fk_firstcatID_cat
   FOREIGN KEY(firstcatID)
   REFERENCES cat(catID),
   CONSTRAINT fk_secondcatID_cat
   FOREIGN KEY(secondcatID)
   REFERENCES cat(catID),
   CONSTRAINT fk_possiblechatID_rerelationshipcats
   FOREIGN KEY(possiblechatID)
   REFERENCES relationshipcats(relationshipcatsID)

        );
insert into relationshipcats values (1, 1, 4, 1);
insert into relationshipcats values (2, 3, 5, 3);
select * from relationshipcats;

ALTER TABLE cat
ADD health varchar(20)

create SEQUENCE serialCat
START WITH 1000
INCREMENT BY 1;

create SEQUENCE relationshipcats_seq
START WITH 1000
INCREMENT BY 1;

ALTER TABLE relationshipcats
DROP COLUMN possiblechatid;

ALTER TABLE cat
ADD sex VARCHAR(100);

