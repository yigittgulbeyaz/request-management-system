--------------------------------------------------------------------------------
-- Request Management System - Generated Volume Seed
-- Target: Oracle Database 12.1.0.2
--
-- Run after 03_seed_data.sql.
--
-- The hand-written seed covers specific scenarios and edge cases. This script
-- adds volume so the data is realistic rather than minimal:
--
--   * 25 additional requests spread across roughly six months, so the monthly
--     volume chart renders several bars rather than one
--   * a majority in CLOSED status, so average resolution time and developer
--     performance are computed from more than a single sample
--   * workflows distributed across all four stages, so the developer task
--     board has rows under every tab
--   * a proportion of TESTING -> IN_PROGRESS transitions, so the test rework
--     rate is a real percentage rather than 0 or 100
--
-- Identity columns are GENERATED ALWAYS, so generated IDs are captured with
-- RETURNING INTO rather than assumed.
--------------------------------------------------------------------------------

DECLARE
TYPE t_ids   IS TABLE OF NUMBER;
    TYPE t_texts IS TABLE OF VARCHAR2(200 CHAR);

    v_customers t_ids;
    v_owners    t_ids;
    v_devs      t_ids;

    v_titles t_texts := t_texts(
        UNISTR('\00D6deme ekran\0131nda tutar yuvarlama hatas\0131'),
        UNISTR('Raporlarda tarih format\0131 yerel ayarlara uymuyor'),
        UNISTR('Toplu kay\0131t y\00FCklemede zaman a\015F\0131m\0131'),
        UNISTR('Arama sonu\00E7lar\0131nda s\0131ralama tutars\0131z'),
        UNISTR('Mobil g\00F6r\00FCn\00FCmde tablo ta\015Fmas\0131'),
        UNISTR('Bildirim zili okunmam\0131\015F say\0131s\0131n\0131 g\00FCncellemiyor'),
        UNISTR('Kullan\0131c\0131 yetkileri de\011Fi\015Fince men\00FC yenilenmiyor'),
        UNISTR('Dosya ekleme alan\0131nda boyut s\0131n\0131r\0131 bildirilmiyor'),
        UNISTR('Oturum s\00FCresi doldu\011Funda veri kayb\0131'),
        UNISTR('Yazd\0131rma \00E7\0131kt\0131s\0131nda kenar bo\015Fluklar\0131 hatal\0131'),
        UNISTR('Filtre temizleme butonu baz\0131 ekranlarda \00E7al\0131\015Fm\0131yor'),
        UNISTR('Grafiklerde eksen etiketleri \00FCst \00FCste biniyor'),
        UNISTR('D\0131\015Fa aktar\0131lan dosyada T\00FCrk\00E7e karakter sorunu'),
        UNISTR('Sayfalama son sayfada bo\015F sonu\00E7 d\00F6nd\00FCr\00FCyor'),
        UNISTR('Otomatik tamamlama \00F6nerileri gecikmeli geliyor'),
        UNISTR('Parola kurallar\0131 formda g\00F6sterilmiyor'),
        UNISTR('Tarih se\00E7icide hafta ba\015Flang\0131c\0131 yanl\0131\015F'),
        UNISTR('Liste ekran\0131nda yenileme sonras\0131 konum kaymas\0131'),
        UNISTR('Yetkisiz eri\015Fim denemesinde hata mesaj\0131 belirsiz'),
        UNISTR('Ekli g\00F6rsellerin \00F6nizlemesi a\00E7\0131lm\0131yor'),
        UNISTR('Kay\0131t silme onay\0131 yanl\0131\015F kayd\0131 g\00F6steriyor'),
        UNISTR('Panel widget s\0131ralamas\0131 kaydedilmiyor'),
        UNISTR('Arama kutusunda \00F6zel karakterler hataya yol a\00E7\0131yor'),
        'Bildirim tercihleri kaydedilmiyor',
        UNISTR('Uzun a\00E7\0131klamalarda metin k\0131rp\0131l\0131yor')
    );

    v_request_id  NUMBER;
    v_status      VARCHAR2(30 CHAR);
    v_created     TIMESTAMP;
    v_closed      TIMESTAMP;
    v_customer    NUMBER;
    v_owner       NUMBER;
    v_dev         NUMBER;
    v_impact      NUMBER;
    v_urgency     NUMBER;
    v_wf_status   VARCHAR2(30 CHAR);
    v_deadline    TIMESTAMP;
    v_age_days    NUMBER;
    v_resolve     NUMBER;
    v_rework      BOOLEAN;

BEGIN
SELECT user_id BULK COLLECT INTO v_customers
FROM yigit_users WHERE role = 'CUSTOMER' AND is_active = 1;

SELECT user_id BULK COLLECT INTO v_owners
FROM yigit_users WHERE role = 'PRODUCT_OWNER' AND is_active = 1;

SELECT user_id BULK COLLECT INTO v_devs
FROM yigit_users WHERE role = 'DEVELOPER' AND is_active = 1;

FOR i IN 1 .. v_titles.COUNT LOOP

        -- Status mix, chosen so there is enough completed work to
        -- average over while the operational screens still have live rows.
        v_status := CASE
                        WHEN i <= 14 THEN 'CLOSED'
                        WHEN i <= 18 THEN 'IN_WORKFLOW'
                        WHEN i <= 21 THEN 'PRIORITIZED'
                        WHEN i <= 23 THEN 'NEW'
                        ELSE 'REJECTED'
END;

        -- Spread creation dates across roughly six months. Ordering by index
        -- keeps the distribution even instead of clustering by chance.
        v_age_days := 175 - (i * 6) + TRUNC(DBMS_RANDOM.VALUE(0, 5));
        v_created  := SYSTIMESTAMP - NUMTODSINTERVAL(v_age_days, 'DAY');

        v_customer := v_customers(TRUNC(DBMS_RANDOM.VALUE(1, v_customers.COUNT + 1)));
        v_owner    := v_owners(TRUNC(DBMS_RANDOM.VALUE(1, v_owners.COUNT + 1)));
        v_dev      := v_devs(TRUNC(DBMS_RANDOM.VALUE(1, v_devs.COUNT + 1)));

        -- Resolution time varies so the average is meaningful rather than flat
        v_resolve := TRUNC(DBMS_RANDOM.VALUE(2, 18));
        v_closed  := v_created + NUMTODSINTERVAL(v_resolve, 'DAY');

        ------------------------------------------------------------------
        -- Request
        ------------------------------------------------------------------
        IF v_status = 'CLOSED' THEN
            INSERT INTO yigit_requests (customer_id, title, description, status,
                                        created_at, closed_at)
            VALUES (v_customer, v_titles(i),
                    UNISTR('Otomatik \00FCretilmi\015F test kayd\0131. ') || v_titles(i) ||
                    UNISTR(' ba\015Fl\0131kl\0131 talep, analitik ekranlar\0131n ger\00E7ek\00E7i veriyle ') ||
                    UNISTR('\00E7al\0131\015Fabilmesi i\00E7in olu\015Fturulmu\015Ftur.'),
                    'CLOSED', v_created, v_closed)
            RETURNING request_id INTO v_request_id;

        ELSIF v_status = 'REJECTED' THEN
            INSERT INTO yigit_requests (customer_id, title, description, status,
                                        rejection_reason, created_at)
            VALUES (v_customer, v_titles(i),
                    UNISTR('Otomatik \00FCretilmi\015F test kayd\0131. ') || v_titles(i) ||
                    UNISTR(' ba\015Fl\0131kl\0131 talep, analitik ekranlar\0131n ger\00E7ek\00E7i veriyle ') ||
                    UNISTR('\00E7al\0131\015Fabilmesi i\00E7in olu\015Fturulmu\015Ftur.'),
                    'REJECTED',
                    UNISTR('Talep mevcut \00FCr\00FCn yol haritas\0131yla \00F6rt\00FC\015Fmedi\011Fi i\00E7in de\011Ferlendirme d\0131\015F\0131 b\0131rak\0131ld\0131.'),
                    v_created)
            RETURNING request_id INTO v_request_id;

ELSE
            INSERT INTO yigit_requests (customer_id, title, description, status,
                                        created_at)
            VALUES (v_customer, v_titles(i),
                    UNISTR('Otomatik \00FCretilmi\015F test kayd\0131. ') || v_titles(i) ||
                    UNISTR(' ba\015Fl\0131kl\0131 talep, analitik ekranlar\0131n ger\00E7ek\00E7i veriyle ') ||
                    UNISTR('\00E7al\0131\015Fabilmesi i\00E7in olu\015Fturulmu\015Ftur.'),
                    v_status, v_created)
            RETURNING request_id INTO v_request_id;
END IF;

        ------------------------------------------------------------------
        -- Creation event
        ------------------------------------------------------------------
INSERT INTO yigit_request_status_history
(request_id, old_status, new_status, changed_by, changed_at)
VALUES (v_request_id, NULL, 'NEW', v_customer, v_created);

------------------------------------------------------------------
-- Prioritization: everything except requests still sitting in NEW
------------------------------------------------------------------
IF v_status <> 'NEW' THEN
            v_impact  := TRUNC(DBMS_RANDOM.VALUE(1, 6));
            v_urgency := TRUNC(DBMS_RANDOM.VALUE(1, 6));

INSERT INTO yigit_priorizations
(request_id, impact, urgency, prioritized_by, created_at)
VALUES (v_request_id, v_impact, v_urgency, v_owner,
        v_created + NUMTODSINTERVAL(1, 'DAY'));

INSERT INTO yigit_request_status_history
(request_id, old_status, new_status, changed_by, changed_at)
VALUES (v_request_id, 'NEW', 'PRIORITIZED', v_owner,
        v_created + NUMTODSINTERVAL(1, 'DAY'));
END IF;

        ------------------------------------------------------------------
        -- Rejection event
        ------------------------------------------------------------------
        IF v_status = 'REJECTED' THEN
            INSERT INTO yigit_request_status_history
                (request_id, old_status, new_status, changed_by, changed_at)
            VALUES (v_request_id, 'PRIORITIZED', 'REJECTED', v_owner,
                    v_created + NUMTODSINTERVAL(2, 'DAY'));
END IF;

        ------------------------------------------------------------------
        -- Workflow, for requests that reached development
        ------------------------------------------------------------------
        IF v_status IN ('IN_WORKFLOW', 'CLOSED') THEN

            IF v_status = 'CLOSED' THEN
                v_wf_status := 'DONE';
ELSE
                -- Live work spread across the three open stages
                v_wf_status := CASE MOD(i, 3)
                                   WHEN 0 THEN 'BACKLOG'
                                   WHEN 1 THEN 'IN_PROGRESS'
                                   ELSE 'TESTING'
END;
END IF;

INSERT INTO yigit_request_status_history
(request_id, old_status, new_status, changed_by, changed_at)
VALUES (v_request_id, 'PRIORITIZED', 'IN_WORKFLOW', v_owner,
        v_created + NUMTODSINTERVAL(2, 'DAY'));

-- The same bands the application uses: two days for critical work,
-- twenty for low. Measured from the conversion rather than from now,
-- so the older rows arrive already overdue and the board has
-- something to colour red.
v_deadline := v_created + NUMTODSINTERVAL(2, 'DAY') + NUMTODSINTERVAL(
                    CASE
                        WHEN v_impact * v_urgency >= 20 THEN 2
                        WHEN v_impact * v_urgency >= 13 THEN 5
                        WHEN v_impact * v_urgency >= 7  THEN 10
                        ELSE 20
                    END, 'DAY');

            IF v_wf_status = 'BACKLOG' THEN
                -- Unassigned tasks are only valid in BACKLOG
                INSERT INTO yigit_workflows
                    (request_id, workflow_status, created_at, deadline)
                VALUES (v_request_id, 'BACKLOG',
                        v_created + NUMTODSINTERVAL(2, 'DAY'), v_deadline);
ELSE
                INSERT INTO yigit_workflows
                    (request_id, developer_id, workflow_status, created_at,
                     assigned_at, deadline)
                VALUES (v_request_id, v_dev, v_wf_status,
                        v_created + NUMTODSINTERVAL(2, 'DAY'),
                        v_created + NUMTODSINTERVAL(3, 'DAY'), v_deadline);

INSERT INTO yigit_request_status_history
(request_id, old_status, new_status, changed_by, changed_at)
VALUES (v_request_id, 'BACKLOG', 'IN_PROGRESS', v_dev,
        v_created + NUMTODSINTERVAL(3, 'DAY'));

IF v_wf_status IN ('TESTING', 'DONE') THEN
                    INSERT INTO yigit_request_status_history
                        (request_id, old_status, new_status, changed_by, changed_at)
                    VALUES (v_request_id, 'IN_PROGRESS', 'TESTING', v_dev,
                            v_created + NUMTODSINTERVAL(4, 'DAY'));

                    -- Roughly one task in three fails testing and returns to
                    -- development. This is the one reverse move the board
                    -- metric counts, so it must not be absent or universal.
                    v_rework := (MOD(i, 3) = 0);

                    IF v_rework THEN
                        INSERT INTO yigit_request_status_history
                            (request_id, old_status, new_status, changed_by, changed_at)
                        VALUES (v_request_id, 'TESTING', 'IN_PROGRESS', v_dev,
                                v_created + NUMTODSINTERVAL(5, 'DAY'));

INSERT INTO yigit_request_status_history
(request_id, old_status, new_status, changed_by, changed_at)
VALUES (v_request_id, 'IN_PROGRESS', 'TESTING', v_dev,
        v_created + NUMTODSINTERVAL(6, 'DAY'));
END IF;
END IF;

                IF v_wf_status = 'DONE' THEN
                    INSERT INTO yigit_request_status_history
                        (request_id, old_status, new_status, changed_by, changed_at)
                    VALUES (v_request_id, 'TESTING', 'DONE', v_dev, v_closed);

INSERT INTO yigit_request_status_history
(request_id, old_status, new_status, changed_by, changed_at)
VALUES (v_request_id, 'IN_WORKFLOW', 'CLOSED', v_dev, v_closed);

-- Completion notice, read on older requests
INSERT INTO yigit_notifications
(user_id, message, is_read, related_request_id, created_at)
VALUES (v_customer, UNISTR('Talebiniz tamamland\0131.'),
        CASE WHEN v_age_days > 60 THEN 1 ELSE 0 END,
        v_request_id, v_closed);
END IF;
END IF;
END IF;

        ------------------------------------------------------------------
        -- Prioritization notice for recent requests only, so the unread
        -- badge stays a plausible number rather than dozens
        ------------------------------------------------------------------
        IF v_status <> 'NEW' AND v_age_days < 45 THEN
            INSERT INTO yigit_notifications
                (user_id, message, related_request_id, created_at)
            VALUES (v_customer, UNISTR('Talebiniz de\011Ferlendirildi ve \00F6nceliklendirildi.'),
                    v_request_id, v_created + NUMTODSINTERVAL(1, 'DAY'));
END IF;

END LOOP;

COMMIT;
END;
/


--------------------------------------------------------------------------------
-- Verification
--------------------------------------------------------------------------------
-- Totals after both seed scripts:
--   SELECT status, COUNT(*) FROM yigit_requests GROUP BY status ORDER BY status;
--     CLOSED 15, IN_WORKFLOW 7, NEW 7, PRIORITIZED 5, REJECTED 3   (37 total)
--
--   SELECT workflow_status, COUNT(*) FROM yigit_workflows
--    GROUP BY workflow_status ORDER BY workflow_status;
--     every stage must have at least one row
--
-- Monthly spread - should return roughly six rows:
--   SELECT TO_CHAR(created_at, 'YYYY-MM') AS month, COUNT(*)
--     FROM yigit_requests GROUP BY TO_CHAR(created_at, 'YYYY-MM') ORDER BY 1;
--
-- Average resolution time in days:
--   SELECT ROUND(AVG(closed_at - created_at), 1) FROM yigit_requests
--    WHERE status = 'CLOSED';
--
-- Returns from testing - should be present but not universal:
--   SELECT ROUND(100 * SUM(CASE WHEN old_status = 'TESTING'
--                                AND new_status = 'IN_PROGRESS' THEN 1 ELSE 0 END)
--                    / NULLIF(SUM(CASE WHEN new_status = 'TESTING' THEN 1 ELSE 0 END), 0), 1)
--     FROM yigit_request_status_history;
--
-- Every score must equal impact * urgency (virtual column check):
--   SELECT COUNT(*) FROM yigit_priorizations
--    WHERE priority_score <> impact * urgency;
--     must return 0
--------------------------------------------------------------------------------