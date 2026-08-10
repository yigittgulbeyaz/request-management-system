--------------------------------------------------------------------------------
-- Request Management System - Development Seed Data
-- Target: Oracle Database 12.1.0.2
--
-- Run after 01_create_tables.sql and 02_indexes.sql.
--
-- Identity columns are GENERATED ALWAYS, so IDs cannot be supplied explicitly.
-- Foreign keys are therefore resolved through natural keys (email, title)
-- rather than hard-coded numbers.
--
-- Development credentials (never used outside local development):
--   customer accounts  -> customer123456
--   product owners     -> po123456
--   developers         -> developer123456
--   administrator      -> admin123456
--   security answer    -> ankara (same for every seeded account)
--------------------------------------------------------------------------------

--------------------------------------------------------------------------------
-- USERS
--------------------------------------------------------------------------------

-- Administrator
INSERT INTO yigit_users (name_surname, email, password_hash, role,
                         security_question, security_answer_hash)
VALUES (UNISTR('Sistem Y\00F6neticisi'), 'admin@company.com',
        '$2a$10$UaRCOTCe7uSyJiUBAalpOOb8X9.yle9N5obZgwyULuBU5dig9OyQu', 'ADMIN',
        'BIRTH_CITY', '$2a$10$qR3ydBHAtmEgpCFKps77W.sZyq7Xlae9eWok8dps/IKu0vmP1LHuW');

-- Product owners
INSERT INTO yigit_users (name_surname, email, password_hash, role,
                         security_question, security_answer_hash)
VALUES ('Elif Kaya', 'elif.kaya@company.com',
        '$2a$10$Favo5DTaii18CErZpEIJl.gsPkWmdPp5O3Rsy5gRwxMOkcUd1MQUu', 'PRODUCT_OWNER',
        'BIRTH_CITY', '$2a$10$qR3ydBHAtmEgpCFKps77W.sZyq7Xlae9eWok8dps/IKu0vmP1LHuW');

INSERT INTO yigit_users (name_surname, email, password_hash, role,
                         security_question, security_answer_hash)
VALUES (UNISTR('Burak \015Eahin'), 'burak.sahin@company.com',
        '$2a$10$Favo5DTaii18CErZpEIJl.gsPkWmdPp5O3Rsy5gRwxMOkcUd1MQUu', 'PRODUCT_OWNER',
        'BIRTH_CITY', '$2a$10$qR3ydBHAtmEgpCFKps77W.sZyq7Xlae9eWok8dps/IKu0vmP1LHuW');

-- Developers
INSERT INTO yigit_users (name_surname, email, password_hash, role,
                         security_question, security_answer_hash)
VALUES (UNISTR('Deniz Y\0131ld\0131r\0131m'), 'deniz.yildirim@company.com',
        '$2a$10$3M7k.GtIV6a8XussCeOfUuQ7ELyaqbDVYpkXOH4er1A68XoZQC7nu', 'DEVELOPER',
        'BIRTH_CITY', '$2a$10$qR3ydBHAtmEgpCFKps77W.sZyq7Xlae9eWok8dps/IKu0vmP1LHuW');

INSERT INTO yigit_users (name_surname, email, password_hash, role,
                         security_question, security_answer_hash)
VALUES (UNISTR('Can \00D6zt\00FCrk'), 'can.ozturk@company.com',
        '$2a$10$3M7k.GtIV6a8XussCeOfUuQ7ELyaqbDVYpkXOH4er1A68XoZQC7nu', 'DEVELOPER',
        'BIRTH_CITY', '$2a$10$qR3ydBHAtmEgpCFKps77W.sZyq7Xlae9eWok8dps/IKu0vmP1LHuW');

INSERT INTO yigit_users (name_surname, email, password_hash, role,
                         security_question, security_answer_hash)
VALUES (UNISTR('Selin Ayd\0131n'), 'selin.aydin@company.com',
        '$2a$10$3M7k.GtIV6a8XussCeOfUuQ7ELyaqbDVYpkXOH4er1A68XoZQC7nu', 'DEVELOPER',
        'BIRTH_CITY', '$2a$10$qR3ydBHAtmEgpCFKps77W.sZyq7Xlae9eWok8dps/IKu0vmP1LHuW');

-- Inactive developer: exercises the soft delete path and must never appear
-- in the assignable developer list
INSERT INTO yigit_users (name_surname, email, password_hash, role, is_active,
                         security_question, security_answer_hash)
VALUES (UNISTR('Kerem Do\011Fan'), 'kerem.dogan@company.com',
        '$2a$10$3M7k.GtIV6a8XussCeOfUuQ7ELyaqbDVYpkXOH4er1A68XoZQC7nu', 'DEVELOPER', 0,
        'BIRTH_CITY', '$2a$10$qR3ydBHAtmEgpCFKps77W.sZyq7Xlae9eWok8dps/IKu0vmP1LHuW');

-- Customers
INSERT INTO yigit_users (name_surname, email, password_hash, role,
                         security_question, security_answer_hash)
VALUES (UNISTR('Ahmet Y\0131lmaz'), 'ahmet.yilmaz@teknocorp.com',
        '$2a$10$ppNVGv95/KvdRvjZ4AXtHOYQGTx0AqxYBxA.ASqYpsqD4DQgL6YQS', 'CUSTOMER',
        'BIRTH_CITY', '$2a$10$qR3ydBHAtmEgpCFKps77W.sZyq7Xlae9eWok8dps/IKu0vmP1LHuW');

INSERT INTO yigit_users (name_surname, email, password_hash, role,
                         security_question, security_answer_hash)
VALUES ('Zeynep Arslan', 'zeynep.arslan@globalas.com',
        '$2a$10$ppNVGv95/KvdRvjZ4AXtHOYQGTx0AqxYBxA.ASqYpsqD4DQgL6YQS', 'CUSTOMER',
        'BIRTH_CITY', '$2a$10$qR3ydBHAtmEgpCFKps77W.sZyq7Xlae9eWok8dps/IKu0vmP1LHuW');

INSERT INTO yigit_users (name_surname, email, password_hash, role,
                         security_question, security_answer_hash)
VALUES (UNISTR('Mehmet \00D6z'), 'mehmet.oz@medyatr.com',
        '$2a$10$ppNVGv95/KvdRvjZ4AXtHOYQGTx0AqxYBxA.ASqYpsqD4DQgL6YQS', 'CUSTOMER',
        'BIRTH_CITY', '$2a$10$qR3ydBHAtmEgpCFKps77W.sZyq7Xlae9eWok8dps/IKu0vmP1LHuW');

INSERT INTO yigit_users (name_surname, email, password_hash, role,
                         security_question, security_answer_hash)
VALUES (UNISTR('Fatma \00C7elik'), 'fatma.celik@medyatr.com',
        '$2a$10$ppNVGv95/KvdRvjZ4AXtHOYQGTx0AqxYBxA.ASqYpsqD4DQgL6YQS', 'CUSTOMER',
        'BIRTH_CITY', '$2a$10$qR3ydBHAtmEgpCFKps77W.sZyq7Xlae9eWok8dps/IKu0vmP1LHuW');

-- Locked customer: exercises the Admin unlock flow
INSERT INTO yigit_users (name_surname, email, password_hash, role,
                         failed_reset_attempts, is_locked,
                         security_question, security_answer_hash)
VALUES ('Okan Erdem', 'okan.erdem@teknocorp.com',
        '$2a$10$ppNVGv95/KvdRvjZ4AXtHOYQGTx0AqxYBxA.ASqYpsqD4DQgL6YQS', 'CUSTOMER',
        3, 1,
        'BIRTH_CITY', '$2a$10$qR3ydBHAtmEgpCFKps77W.sZyq7Xlae9eWok8dps/IKu0vmP1LHuW');

-- Customer with no requests at all: the only way to exercise the empty state
-- on the "My Requests" screen. Deliberately left without any request rows.
INSERT INTO yigit_users (name_surname, email, password_hash, role,
                         security_question, security_answer_hash)
VALUES ('Pelin Kurt', 'pelin.kurt@globalas.com',
        '$2a$10$ppNVGv95/KvdRvjZ4AXtHOYQGTx0AqxYBxA.ASqYpsqD4DQgL6YQS', 'CUSTOMER',
        'BIRTH_CITY', '$2a$10$qR3ydBHAtmEgpCFKps77W.sZyq7Xlae9eWok8dps/IKu0vmP1LHuW');

-- Customer holding a temporary password: on login this account must be
-- redirected to the forced password change screen and blocked from every
-- other route until the password is set.
INSERT INTO yigit_users (name_surname, email, password_hash, role,
                         must_change_password,
                         security_question, security_answer_hash)
VALUES ('Serkan Bulut', 'serkan.bulut@teknocorp.com',
        '$2a$10$ppNVGv95/KvdRvjZ4AXtHOYQGTx0AqxYBxA.ASqYpsqD4DQgL6YQS', 'CUSTOMER',
        1,
        'BIRTH_CITY', '$2a$10$qR3ydBHAtmEgpCFKps77W.sZyq7Xlae9eWok8dps/IKu0vmP1LHuW');

-- Deactivated customer: must not be able to log in, but the name must still
-- resolve on the request they submitted while active.
INSERT INTO yigit_users (name_surname, email, password_hash, role, is_active,
                         security_question, security_answer_hash)
VALUES (UNISTR('G\00FCl\015Fah Tun\00E7'), 'gulsah.tunc@medyatr.com',
        '$2a$10$ppNVGv95/KvdRvjZ4AXtHOYQGTx0AqxYBxA.ASqYpsqD4DQgL6YQS', 'CUSTOMER', 0,
        'BIRTH_CITY', '$2a$10$qR3ydBHAtmEgpCFKps77W.sZyq7Xlae9eWok8dps/IKu0vmP1LHuW');

-- Customer with non-default preferences: proves the stored theme and language
-- are actually read at layout construction rather than assumed.
INSERT INTO yigit_users (name_surname, email, password_hash, role,
                         preferred_theme, preferred_language,
                         security_question, security_answer_hash)
VALUES ('John Carter', 'john.carter@globalas.com',
        '$2a$10$ppNVGv95/KvdRvjZ4AXtHOYQGTx0AqxYBxA.ASqYpsqD4DQgL6YQS', 'CUSTOMER',
        'dark', 'en',
        'BIRTH_CITY', '$2a$10$qR3ydBHAtmEgpCFKps77W.sZyq7Xlae9eWok8dps/IKu0vmP1LHuW');


--------------------------------------------------------------------------------
-- REQUESTS
--
-- Coverage: every status appears at least once, so each screen has data.
--------------------------------------------------------------------------------

-- NEW: awaiting prioritization, appear as "Not Assigned" in the PO pool
INSERT INTO yigit_requests (customer_id, title, description, status, created_at)
VALUES ((SELECT user_id FROM yigit_users WHERE email = 'mehmet.oz@medyatr.com'),
        UNISTR('Buton Hizalama \0130\015Fi ve Mobil Men\00FC Kaymas\0131'),
        UNISTR('Mobil g\00F6r\00FCn\00FCmde \00FCst men\00FC sa\011Fa kay\0131yor ve g\00F6nder butonu form alan\0131n\0131n d\0131\015F\0131na ta\015F\0131yor. iPhone ve Android cihazlarda tekrarlanabiliyor.'),
        'NEW', SYSTIMESTAMP - 2);

INSERT INTO yigit_requests (customer_id, title, description, status, created_at)
VALUES ((SELECT user_id FROM yigit_users WHERE email = 'fatma.celik@medyatr.com'),
        UNISTR('Kullan\0131c\0131 Profil Foto\011Fraf\0131 Y\00FCklenemiyor'),
        UNISTR('Profil ayarlar\0131 ekran\0131ndan foto\011Fraf y\00FCklemeye \00E7al\0131\015Ft\0131\011F\0131m\0131zda y\00FCkleme \00E7ubu\011Fu doluyor ancak i\015Flem tamamlanm\0131yor. Sayfa yenilendi\011Finde eski foto\011Fraf g\00F6r\00FCnmeye devam ediyor.'),
        'NEW', SYSTIMESTAMP - 1);

INSERT INTO yigit_requests (customer_id, title, description, status, created_at)
VALUES ((SELECT user_id FROM yigit_users WHERE email = 'ahmet.yilmaz@teknocorp.com'),
        UNISTR('Bildirim E-postalar\0131nda T\00FCrk\00E7e Karakter Bozulmas\0131'),
        UNISTR('Sistemden g\00F6nderilen bilgilendirme e-postalar\0131nda T\00FCrk\00E7e karakterler bozuk g\00F6r\00FCn\00FCyor. \00D6zellikle \015F, \011F ve \0131 harfleri soru i\015Fareti olarak geliyor.'),
        'NEW', SYSTIMESTAMP - 0.5);

-- PRIORITIZED: scored, awaiting conversion to a workflow
INSERT INTO yigit_requests (customer_id, title, description, status, created_at)
VALUES ((SELECT user_id FROM yigit_users WHERE email = 'zeynep.arslan@globalas.com'),
        UNISTR('Excel Rapor \00C7\0131kt\0131s\0131 Al\0131nam\0131yor'),
        UNISTR('Ayl\0131k sat\0131\015F raporunu Excel olarak d\0131\015Fa aktarmaya \00E7al\0131\015Ft\0131\011F\0131m\0131zda dosya indiriliyor ancak a\00E7\0131lm\0131yor. Dosya boyutu 0 KB g\00F6r\00FCn\00FCyor. Rapor ekran\0131ndaki di\011Fer d\0131\015Fa aktarma se\00E7enekleri sorunsuz \00E7al\0131\015F\0131yor.'),
        'PRIORITIZED', SYSTIMESTAMP - 6);

INSERT INTO yigit_requests (customer_id, title, description, status, created_at)
VALUES ((SELECT user_id FROM yigit_users WHERE email = 'mehmet.oz@medyatr.com'),
        UNISTR('Renk Temas\0131 De\011Fi\015Fimi Talebi'),
        UNISTR('Kurumsal kimli\011Fimiz g\00FCncellendi. Uygulamadaki ana renklerin yeni marka rehberimize g\00F6re g\00FCncellenmesini talep ediyoruz.'),
        'PRIORITIZED', SYSTIMESTAMP - 8);

-- IN_WORKFLOW: converted, development in progress
INSERT INTO yigit_requests (customer_id, title, description, status, created_at)
VALUES ((SELECT user_id FROM yigit_users WHERE email = 'ahmet.yilmaz@teknocorp.com'),
        UNISTR('Giri\015F API Hatas\0131'),
        UNISTR('Yo\011Fun saatlerde giri\015F yapmaya \00E7al\0131\015Fan kullan\0131c\0131lar zaman a\015F\0131m\0131 hatas\0131 al\0131yor. Sorun g\00FCnde birka\00E7 kez tekrarlan\0131yor ve t\00FCm kullan\0131c\0131lar\0131 etkiliyor. Acil \00E7\00F6z\00FCm gerekiyor.'),
        'IN_WORKFLOW', SYSTIMESTAMP - 12);

INSERT INTO yigit_requests (customer_id, title, description, status, created_at)
VALUES ((SELECT user_id FROM yigit_users WHERE email = 'zeynep.arslan@globalas.com'),
        UNISTR('Sipari\015F Listesinde Filtreleme Yava\015Fl\0131\011F\0131'),
        UNISTR('Sipari\015F listesinde tarih aral\0131\011F\0131 filtresi uyguland\0131\011F\0131nda sonu\00E7lar\0131n gelmesi 30 saniyeden uzun s\00FCr\00FCyor. Kay\0131t say\0131s\0131 artt\0131k\00E7a durum k\00F6t\00FCle\015Fiyor.'),
        'IN_WORKFLOW', SYSTIMESTAMP - 10);

INSERT INTO yigit_requests (customer_id, title, description, status, created_at)
VALUES ((SELECT user_id FROM yigit_users WHERE email = 'fatma.celik@medyatr.com'),
        UNISTR('Fatura PDF \015Eablonunda Logo G\00F6r\00FCnm\00FCyor'),
        UNISTR('Olu\015Fturulan fatura PDF \00E7\0131kt\0131lar\0131nda \015Firket logosu g\00F6r\00FCnm\00FCyor, yerine bo\015F bir alan kal\0131yor. Ekran \00F6nizlemesinde logo do\011Fru g\00F6r\00FCn\00FCyor.'),
        'IN_WORKFLOW', SYSTIMESTAMP - 9);

-- CLOSED: completed. closed_at is mandatory for this status.
INSERT INTO yigit_requests (customer_id, title, description, status, created_at, closed_at)
VALUES ((SELECT user_id FROM yigit_users WHERE email = 'ahmet.yilmaz@teknocorp.com'),
        UNISTR('\015Eifre S\0131f\0131rlama Ba\011Flant\0131s\0131 \00C7al\0131\015Fm\0131yor'),
        UNISTR('\015Eifremi unuttum ak\0131\015F\0131nda gelen ba\011Flant\0131ya t\0131kland\0131\011F\0131nda hata sayfas\0131 a\00E7\0131l\0131yordu.'),
        'CLOSED', SYSTIMESTAMP - 20, SYSTIMESTAMP - 14);

-- REJECTED: dead end, rejection_reason is shown to the customer
INSERT INTO yigit_requests (customer_id, title, description, status, rejection_reason, created_at)
VALUES ((SELECT user_id FROM yigit_users WHERE email = 'mehmet.oz@medyatr.com'),
        UNISTR('Ana Sayfaya Animasyonlu Kar\015F\0131lama Ekran\0131'),
        UNISTR('Uygulama a\00E7\0131l\0131\015F\0131nda animasyonlu bir kar\015F\0131lama ekran\0131 g\00F6sterilmesini istiyoruz.'),
        'REJECTED',
        UNISTR('Talep, a\00E7\0131l\0131\015F performans\0131n\0131 olumsuz etkileyece\011Fi i\00E7in de\011Ferlendirme d\0131\015F\0131 b\0131rak\0131ld\0131. Benzer ihtiya\00E7 i\00E7in mevcut bildirim alan\0131 kullan\0131labilir.'),
        SYSTIMESTAMP - 15);

--------------------------------------------------------------------------------
-- Boundary cases
--------------------------------------------------------------------------------

-- Title close to the 200 character limit, description well past the 4000 byte
-- VARCHAR2 ceiling. Together these prove the column sizing decisions hold:
-- VARCHAR2(200 CHAR) for the title, CLOB for the description. The title is also
-- dense with Turkish characters, which is what CHAR semantics were chosen for.
INSERT INTO yigit_requests (customer_id, title, description, status, created_at)
VALUES ((SELECT user_id FROM yigit_users WHERE email = 'ahmet.yilmaz@teknocorp.com'),
        UNISTR('Sipari\015F olu\015Fturma ekran\0131nda \00F6deme ad\0131m\0131na ge\00E7ildi\011Finde a\00E7\0131lan \00FC\00E7\00FCnc\00FC taraf do\011Frulama penceresi baz\0131 taray\0131c\0131larda g\00F6r\00FCnm\00FCyor ve i\015Flem yar\0131da kal\0131yor; kullan\0131c\0131lar tekrar denerse \00E7ift kay\0131t olu\015Fuyor'),
        TO_CLOB(RPAD(UNISTR('Sorun ilk olarak ge\00E7en hafta fark edildi ve o tarihten bu yana d\00FCzenli olarak tekrarlan\0131yor. '),
                     3000,
                     UNISTR('Kullan\0131c\0131 sepete \00FCr\00FCn ekliyor, \00F6deme ad\0131m\0131na ge\00E7iyor ve do\011Frulama penceresi bekleniyor. ')))
          || TO_CLOB(RPAD(UNISTR('Baz\0131 taray\0131c\0131larda pencere hi\00E7 a\00E7\0131lm\0131yor, baz\0131lar\0131nda bo\015F bir \00E7er\00E7eve g\00F6r\00FCn\00FCyor. '),
                          2000,
                          UNISTR('Tekrarlanabilirlik oran\0131 y\00FCzde altm\0131\015F olarak \00F6l\00E7\00FCld\00FC. '))),
        'NEW', SYSTIMESTAMP - 3);

-- Request submitted by the now-deactivated customer. Deactivation is a soft
-- delete, so this row must still resolve the customer name on the PO pool.
INSERT INTO yigit_requests (customer_id, title, description, status, created_at)
VALUES ((SELECT user_id FROM yigit_users WHERE email = 'gulsah.tunc@medyatr.com'),
        UNISTR('Ar\015Fiv Sayfas\0131nda Tarih Filtresi Hatal\0131 Sonu\00E7 Veriyor'),
        UNISTR('Ar\015Fiv ekran\0131nda iki tarih aras\0131nda filtreleme yap\0131ld\0131\011F\0131nda aral\0131\011F\0131n son g\00FCn\00FCne ait kay\0131tlar listeye dahil edilmiyor.'),
        'NEW', SYSTIMESTAMP - 22);


--------------------------------------------------------------------------------
-- PRIORIZATIONS
--
-- priority_score is a virtual column and is never supplied here.
-- Scores cover all three bands: Low (1-6), Medium (7-15), Critical (16-25).
--------------------------------------------------------------------------------

-- Excel report: 3 x 4 = 12 (Medium)
INSERT INTO yigit_priorizations (request_id, impact, urgency, prioritized_by)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('Excel Rapor \00C7\0131kt\0131s\0131 Al\0131nam\0131yor')),
        3, 4,
        (SELECT user_id FROM yigit_users WHERE email = 'elif.kaya@company.com'));

-- Colour theme: 1 x 2 = 2 (Low)
INSERT INTO yigit_priorizations (request_id, impact, urgency, prioritized_by)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('Renk Temas\0131 De\011Fi\015Fimi Talebi')),
        1, 2,
        (SELECT user_id FROM yigit_users WHERE email = 'elif.kaya@company.com'));

-- Login API failure: 5 x 5 = 25 (Critical)
INSERT INTO yigit_priorizations (request_id, impact, urgency, prioritized_by)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('Giri\015F API Hatas\0131')),
        5, 5,
        (SELECT user_id FROM yigit_users WHERE email = 'elif.kaya@company.com'));

-- Order filtering: 4 x 3 = 12 (Medium)
INSERT INTO yigit_priorizations (request_id, impact, urgency, prioritized_by)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('Sipari\015F Listesinde Filtreleme Yava\015Fl\0131\011F\0131')),
        4, 3,
        (SELECT user_id FROM yigit_users WHERE email = 'burak.sahin@company.com'));

-- Invoice logo: 2 x 3 = 6 (Low)
INSERT INTO yigit_priorizations (request_id, impact, urgency, prioritized_by)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('Fatura PDF \015Eablonunda Logo G\00F6r\00FCnm\00FCyor')),
        2, 3,
        (SELECT user_id FROM yigit_users WHERE email = 'burak.sahin@company.com'));

-- Closed request retains its score: 4 x 4 = 16 (Critical)
INSERT INTO yigit_priorizations (request_id, impact, urgency, prioritized_by)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('\015Eifre S\0131f\0131rlama Ba\011Flant\0131s\0131 \00C7al\0131\015Fm\0131yor')),
        4, 4,
        (SELECT user_id FROM yigit_users WHERE email = 'elif.kaya@company.com'));


--------------------------------------------------------------------------------
-- WORKFLOWS
--
-- developer_id and assigned_at are set together or both left null.
-- An unassigned task can only sit in BACKLOG.
--------------------------------------------------------------------------------

-- IN_PROGRESS, assigned
INSERT INTO yigit_workflows (request_id, developer_id, workflow_status, created_at, assigned_at)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('Giri\015F API Hatas\0131')),
        (SELECT user_id FROM yigit_users WHERE email = 'deniz.yildirim@company.com'),
        'IN_PROGRESS', SYSTIMESTAMP - 5, SYSTIMESTAMP - 5);

-- TESTING, assigned
INSERT INTO yigit_workflows (request_id, developer_id, workflow_status, created_at, assigned_at)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('Sipari\015F Listesinde Filtreleme Yava\015Fl\0131\011F\0131')),
        (SELECT user_id FROM yigit_users WHERE email = 'can.ozturk@company.com'),
        'TESTING', SYSTIMESTAMP - 4, SYSTIMESTAMP - 4);

-- BACKLOG, unassigned: available for a developer to claim
INSERT INTO yigit_workflows (request_id, workflow_status, created_at)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('Fatura PDF \015Eablonunda Logo G\00F6r\00FCnm\00FCyor')),
        'BACKLOG', SYSTIMESTAMP - 3);

-- DONE, matching the CLOSED request
INSERT INTO yigit_workflows (request_id, developer_id, workflow_status, created_at, assigned_at)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('\015Eifre S\0131f\0131rlama Ba\011Flant\0131s\0131 \00C7al\0131\015Fm\0131yor')),
        (SELECT user_id FROM yigit_users WHERE email = 'selin.aydin@company.com'),
        'DONE', SYSTIMESTAMP - 18, SYSTIMESTAMP - 18);


--------------------------------------------------------------------------------
-- REQUEST_STATUS_HISTORY
--
-- Append-only audit trail. Covers both request-level and workflow-level
-- transitions, since the analytics read both from this table.
--------------------------------------------------------------------------------

-- Full lifecycle of the closed request
INSERT INTO yigit_request_status_history (request_id, old_status, new_status, changed_by, changed_at)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('\015Eifre S\0131f\0131rlama Ba\011Flant\0131s\0131 \00C7al\0131\015Fm\0131yor')),
        NULL, 'NEW',
        (SELECT user_id FROM yigit_users WHERE email = 'ahmet.yilmaz@teknocorp.com'),
        SYSTIMESTAMP - 20);

INSERT INTO yigit_request_status_history (request_id, old_status, new_status, changed_by, changed_at)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('\015Eifre S\0131f\0131rlama Ba\011Flant\0131s\0131 \00C7al\0131\015Fm\0131yor')),
        'NEW', 'PRIORITIZED',
        (SELECT user_id FROM yigit_users WHERE email = 'elif.kaya@company.com'),
        SYSTIMESTAMP - 19);

INSERT INTO yigit_request_status_history (request_id, old_status, new_status, changed_by, changed_at)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('\015Eifre S\0131f\0131rlama Ba\011Flant\0131s\0131 \00C7al\0131\015Fm\0131yor')),
        'PRIORITIZED', 'IN_WORKFLOW',
        (SELECT user_id FROM yigit_users WHERE email = 'elif.kaya@company.com'),
        SYSTIMESTAMP - 18);

INSERT INTO yigit_request_status_history (request_id, old_status, new_status, changed_by, changed_at)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('\015Eifre S\0131f\0131rlama Ba\011Flant\0131s\0131 \00C7al\0131\015Fm\0131yor')),
        'BACKLOG', 'IN_PROGRESS',
        (SELECT user_id FROM yigit_users WHERE email = 'selin.aydin@company.com'),
        SYSTIMESTAMP - 17);

INSERT INTO yigit_request_status_history (request_id, old_status, new_status, changed_by, changed_at)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('\015Eifre S\0131f\0131rlama Ba\011Flant\0131s\0131 \00C7al\0131\015Fm\0131yor')),
        'IN_PROGRESS', 'TESTING',
        (SELECT user_id FROM yigit_users WHERE email = 'selin.aydin@company.com'),
        SYSTIMESTAMP - 16);

-- Test failure and rework: the transition the rework rate metric counts
INSERT INTO yigit_request_status_history (request_id, old_status, new_status, changed_by, changed_at)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('\015Eifre S\0131f\0131rlama Ba\011Flant\0131s\0131 \00C7al\0131\015Fm\0131yor')),
        'TESTING', 'IN_PROGRESS',
        (SELECT user_id FROM yigit_users WHERE email = 'selin.aydin@company.com'),
        SYSTIMESTAMP - 15.5);

INSERT INTO yigit_request_status_history (request_id, old_status, new_status, changed_by, changed_at)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('\015Eifre S\0131f\0131rlama Ba\011Flant\0131s\0131 \00C7al\0131\015Fm\0131yor')),
        'IN_PROGRESS', 'TESTING',
        (SELECT user_id FROM yigit_users WHERE email = 'selin.aydin@company.com'),
        SYSTIMESTAMP - 15);

INSERT INTO yigit_request_status_history (request_id, old_status, new_status, changed_by, changed_at)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('\015Eifre S\0131f\0131rlama Ba\011Flant\0131s\0131 \00C7al\0131\015Fm\0131yor')),
        'TESTING', 'DONE',
        (SELECT user_id FROM yigit_users WHERE email = 'selin.aydin@company.com'),
        SYSTIMESTAMP - 14);

INSERT INTO yigit_request_status_history (request_id, old_status, new_status, changed_by, changed_at)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('\015Eifre S\0131f\0131rlama Ba\011Flant\0131s\0131 \00C7al\0131\015Fm\0131yor')),
        'IN_WORKFLOW', 'CLOSED',
        (SELECT user_id FROM yigit_users WHERE email = 'selin.aydin@company.com'),
        SYSTIMESTAMP - 14);

-- Rejected request
INSERT INTO yigit_request_status_history (request_id, old_status, new_status, changed_by, changed_at)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('Ana Sayfaya Animasyonlu Kar\015F\0131lama Ekran\0131')),
        NULL, 'NEW',
        (SELECT user_id FROM yigit_users WHERE email = 'mehmet.oz@medyatr.com'),
        SYSTIMESTAMP - 15);

INSERT INTO yigit_request_status_history (request_id, old_status, new_status, changed_by, changed_at)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('Ana Sayfaya Animasyonlu Kar\015F\0131lama Ekran\0131')),
        'NEW', 'REJECTED',
        (SELECT user_id FROM yigit_users WHERE email = 'burak.sahin@company.com'),
        SYSTIMESTAMP - 13);

-- Login API failure, currently in progress
INSERT INTO yigit_request_status_history (request_id, old_status, new_status, changed_by, changed_at)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('Giri\015F API Hatas\0131')),
        'PRIORITIZED', 'IN_WORKFLOW',
        (SELECT user_id FROM yigit_users WHERE email = 'elif.kaya@company.com'),
        SYSTIMESTAMP - 5);

INSERT INTO yigit_request_status_history (request_id, old_status, new_status, changed_by, changed_at)
VALUES ((SELECT request_id FROM yigit_requests WHERE title = UNISTR('Giri\015F API Hatas\0131')),
        'BACKLOG', 'IN_PROGRESS',
        (SELECT user_id FROM yigit_users WHERE email = 'deniz.yildirim@company.com'),
        SYSTIMESTAMP - 5);


--------------------------------------------------------------------------------
-- NOTIFICATIONS
--------------------------------------------------------------------------------

-- Unread: customer told their request was scored
INSERT INTO yigit_notifications (user_id, message, related_request_id, created_at)
VALUES ((SELECT user_id FROM yigit_users WHERE email = 'zeynep.arslan@globalas.com'),
        UNISTR('Talebiniz de\011Ferlendirildi ve \00F6nceliklendirildi.'),
        (SELECT request_id FROM yigit_requests WHERE title = UNISTR('Excel Rapor \00C7\0131kt\0131s\0131 Al\0131nam\0131yor')),
        SYSTIMESTAMP - 6);

-- Unread: developer told a task was assigned
INSERT INTO yigit_notifications (user_id, message, related_request_id, created_at)
VALUES ((SELECT user_id FROM yigit_users WHERE email = 'deniz.yildirim@company.com'),
        UNISTR('Size yeni bir g\00F6rev atand\0131: Giri\015F API Hatas\0131'),
        (SELECT request_id FROM yigit_requests WHERE title = UNISTR('Giri\015F API Hatas\0131')),
        SYSTIMESTAMP - 5);

-- Unread: rejection notice
INSERT INTO yigit_notifications (user_id, message, related_request_id, created_at)
VALUES ((SELECT user_id FROM yigit_users WHERE email = 'mehmet.oz@medyatr.com'),
        UNISTR('Talebiniz de\011Ferlendirme d\0131\015F\0131 b\0131rak\0131ld\0131.'),
        (SELECT request_id FROM yigit_requests WHERE title = UNISTR('Ana Sayfaya Animasyonlu Kar\015F\0131lama Ekran\0131')),
        SYSTIMESTAMP - 13);

-- Read: completion notice
INSERT INTO yigit_notifications (user_id, message, is_read, related_request_id, created_at)
VALUES ((SELECT user_id FROM yigit_users WHERE email = 'ahmet.yilmaz@teknocorp.com'),
        UNISTR('Talebiniz tamamland\0131.'), 1,
        (SELECT request_id FROM yigit_requests WHERE title = UNISTR('\015Eifre S\0131f\0131rlama Ba\011Flant\0131s\0131 \00C7al\0131\015Fm\0131yor')),
        SYSTIMESTAMP - 14);

-- Read: assignment notice
INSERT INTO yigit_notifications (user_id, message, is_read, related_request_id, created_at)
VALUES ((SELECT user_id FROM yigit_users WHERE email = 'can.ozturk@company.com'),
        UNISTR('Size yeni bir g\00F6rev atand\0131: Sipari\015F Listesinde Filtreleme Yava\015Fl\0131\011F\0131'), 1,
        (SELECT request_id FROM yigit_requests WHERE title = UNISTR('Sipari\015F Listesinde Filtreleme Yava\015Fl\0131\011F\0131')),
        SYSTIMESTAMP - 4);


COMMIT;


--------------------------------------------------------------------------------
-- Verification
--------------------------------------------------------------------------------
-- SELECT status, COUNT(*) FROM yigit_requests GROUP BY status;
--   NEW 5, PRIORITIZED 2, IN_WORKFLOW 3, CLOSED 1, REJECTED 1
--
-- Boundary case checks:
--   SELECT LENGTH(title) FROM yigit_requests ORDER BY LENGTH(title) DESC FETCH FIRST 1 ROW ONLY;
--     close to 200, and must not have been truncated
--   SELECT DBMS_LOB.GETLENGTH(description) FROM yigit_requests
--     ORDER BY DBMS_LOB.GETLENGTH(description) DESC FETCH FIRST 1 ROW ONLY;
--     above 4000, proving CLOB was the right choice over VARCHAR2
--
-- Users without requests (empty state fixture):
--   SELECT u.email FROM yigit_users u
--    WHERE u.role = 'CUSTOMER'
--      AND NOT EXISTS (SELECT 1 FROM yigit_requests r WHERE r.customer_id = u.user_id);
--     pelin.kurt@globalas.com, serkan.bulut@teknocorp.com, okan.erdem@teknocorp.com,
--     john.carter@globalas.com
--
-- SELECT r.title, p.impact, p.urgency, p.priority_score
--   FROM yigit_requests r JOIN yigit_priorizations p ON p.request_id = r.request_id;
--   priority_score must equal impact * urgency for every row
--
-- SELECT workflow_status, COUNT(*) FROM yigit_workflows GROUP BY workflow_status;
--   BACKLOG 1, IN_PROGRESS 1, TESTING 1, DONE 1
--------------------------------------------------------------------------------
