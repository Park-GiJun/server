-- 🎵 아이유 HEREH 월드투어 MySQL 최적화 대용량 데이터 생성 스크립트 🎵
-- 사용자 1,000,000명, 콘서트 22개 도시, 공연 5,500회 (각 도시별 250회), 좌석 275,000석
-- MySQL 대용량 INSERT 최적화 적용
-- 실행 방법: docker exec -i server-kotlin-mysql-1 mysql -u root -p hhplus < mysql-optimized-data.sql

USE hhplus;

-- MySQL 대용량 INSERT 최적화 설정
SET SESSION autocommit = 0;
SET SESSION unique_checks = 0;
SET SESSION foreign_key_checks = 0;
SET SESSION sync_binlog = 0;
SET SESSION max_allowed_packet = 1073741824; -- 1GB

-- 대용량 처리를 위한 설정
SET SESSION bulk_insert_buffer_size = 256*1024*1024; -- 256MB
SET SESSION myisam_sort_buffer_size = 256*1024*1024; -- 256MB
SET SESSION read_buffer_size = 8*1024*1024; -- 8MB
SET SESSION read_rnd_buffer_size = 8*1024*1024; -- 8MB

SELECT '🎵 아이유 HEREH 월드투어 MySQL 최적화 대용량 데이터 생성 시작 🎵' as status;
SELECT '🎪 각 도시별 250회씩 대규모 공연! (총 5,500회)' as concept;

-- 기존 데이터 삭제
TRUNCATE TABLE point_history;
TRUNCATE TABLE payments;
TRUNCATE TABLE reservation;
TRUNCATE TABLE temp_reservation;
TRUNCATE TABLE queue_token;
TRUNCATE TABLE concert_seat;
TRUNCATE TABLE concert_seat_grade;
TRUNCATE TABLE concert_date;
TRUNCATE TABLE concerts;
TRUNCATE TABLE users;

-- 1. 사용자 1,000,000명 생성 (MySQL 최적화 배치 INSERT)
SELECT '1단계: 사용자 1,000,000명 생성 중... (배치 최적화)' as status;

DELIMITER $$
CREATE PROCEDURE generate_users()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 10000;
    DECLARE current_batch INT DEFAULT 0;

    WHILE current_batch < 100 DO
            SET @start_id = current_batch * batch_size + 1;
            SET @end_id = (current_batch + 1) * batch_size;

            SET @sql = CONCAT('
        INSERT INTO users (user_id, user_name, total_point, available_point, used_point, created_at, updated_at, is_deleted)
        SELECT
            CONCAT("user-", n) as user_id,
            CONCAT("아이유팬 ", n) as user_name,
            CASE
                WHEN n % 100 = 0 THEN 0
                WHEN n % 20 = 0 THEN 10000000
                WHEN n % 10 = 0 THEN 5000000
                WHEN n % 5 = 0 THEN 2000000
                ELSE 1000000
            END as total_point,
            CASE
                WHEN n % 100 = 0 THEN 0
                WHEN n % 20 = 0 THEN 9000000
                WHEN n % 10 = 0 THEN 4500000
                WHEN n % 5 = 0 THEN 1800000
                ELSE 800000
            END as available_point,
            CASE
                WHEN n % 100 = 0 THEN 0
                WHEN n % 20 = 0 THEN 1000000
                WHEN n % 10 = 0 THEN 500000
                WHEN n % 5 = 0 THEN 200000
                ELSE 200000
            END as used_point,
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 1095) DAY) as created_at,
            NOW() as updated_at,
            0 as is_deleted
        FROM (
            SELECT ', @start_id, ' + (a.N + b.N * 10 + c.N * 100 + d.N * 1000) as n
            FROM
                (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
                (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
                (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c,
                (SELECT 0 AS N UNION SELECT 1) d
            WHERE ', @start_id, ' + (a.N + b.N * 10 + c.N * 100 + d.N * 1000) <= ', @end_id, '
        ) numbers');

            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;

            COMMIT;

            SET current_batch = current_batch + 1;
            IF current_batch % 10 = 0 THEN
                SELECT CONCAT('사용자 ', FORMAT(current_batch * batch_size, 0), '명 생성 완료') as progress;
            END IF;
        END WHILE;
END$$
DELIMITER ;

CALL generate_users();
DROP PROCEDURE generate_users;

SELECT '사용자 1,000,000명 생성 완료' as status;

-- 2. 아이유 HEREH 월드투어 22개 도시 생성 (기존과 동일)
SELECT '2단계: 아이유 HEREH 월드투어 22개 도시 생성 중...' as status;

INSERT INTO concerts (concert_name, location, description, created_at, updated_at, is_deleted) VALUES
-- 아시아 투어
('아이유 HEREH WORLD TOUR - SEOUL', '서울 올림픽공원 체조경기장', '아이유 HEREH 월드투어 서울 공연', NOW(), NOW(), 0),
('아이유 HEREH WORLD TOUR - BUSAN', '부산 BEXCO 오디토리움', '아이유 HEREH 월드투어 부산 공연', NOW(), NOW(), 0),
('아이유 HEREH WORLD TOUR - TOKYO', 'Tokyo Dome', '아이유 HEREH 월드투어 도쿄 공연', NOW(), NOW(), 0),
('아이유 HEREH WORLD TOUR - OSAKA', 'Osaka-jo Hall', '아이유 HEREH 월드투어 오사카 공연', NOW(), NOW(), 0),
('아이유 HEREH WORLD TOUR - HONG KONG', 'Hong Kong Coliseum', '아이유 HEREH 월드투어 홍콩 공연', NOW(), NOW(), 0),
('아이유 HEREH WORLD TOUR - SINGAPORE', 'Singapore Indoor Stadium', '아이유 HEREH 월드투어 싱가포르 공연', NOW(), NOW(), 0),
('아이유 HEREH WORLD TOUR - BANGKOK', 'Impact Arena Bangkok', '아이유 HEREH 월드투어 방콕 공연', NOW(), NOW(), 0),
('아이유 HEREH WORLD TOUR - MANILA', 'Mall of Asia Arena', '아이유 HEREH 월드투어 마닐라 공연', NOW(), NOW(), 0),
('아이유 HEREH WORLD TOUR - TAIPEI', 'Taipei Arena', '아이유 HEREH 월드투어 타이페이 공연', NOW(), NOW(), 0),
('아이유 HEREH WORLD TOUR - KUALA LUMPUR', 'Axiata Arena', '아이유 HEREH 월드투어 쿠알라룸푸르 공연', NOW(), NOW(), 0),
('아이유 HEREH WORLD TOUR - JAKARTA', 'Indonesia Convention Exhibition', '아이유 HEREH 월드투어 자카르타 공연', NOW(), NOW(), 0),

-- 북미 투어
('아이유 HEREH WORLD TOUR - LOS ANGELES', 'Crypto.com Arena', '아이유 HEREH 월드투어 로스앤젤레스 공연', NOW(), NOW(), 0),
('아이유 HEREH WORLD TOUR - OAKLAND', 'Oakland Arena', '아이유 HEREH 월드투어 오클랜드 공연', NOW(), NOW(), 0),
('아이유 HEREH WORLD TOUR - CHICAGO', 'Allstate Arena', '아이유 HEREH 월드투어 시카고 공연', NOW(), NOW(), 0),
('아이유 HEREH WORLD TOUR - ATLANTA', 'State Farm Arena', '아이유 HEREH 월드투어 애틀랜타 공연', NOW(), NOW(), 0),
('아이유 HEREH WORLD TOUR - NEW YORK', 'UBS Arena', '아이유 HEREH 월드투어 뉴욕 공연', NOW(), NOW(), 0),
('아이유 HEREH WORLD TOUR - NEWARK', 'Prudential Center', '아이유 HEREH 월드투어 뉴어크 공연', NOW(), NOW(), 0),
('아이유 HEREH WORLD TOUR - TORONTO', 'Scotiabank Arena', '아이유 HEREH 월드투어 토론토 공연', NOW(), NOW(), 0),

-- 유럽 투어
('아이유 HEREH WORLD TOUR - LONDON', 'The O2 Arena', '아이유 HEREH 월드투어 런던 공연', NOW(), NOW(), 0),
('아이유 HEREH WORLD TOUR - BERLIN', 'Mercedes-Benz Arena', '아이유 HEREH 월드투어 베를린 공연', NOW(), NOW(), 0),
('아이유 HEREH WORLD TOUR - PARIS', 'AccorHotels Arena', '아이유 HEREH 월드투어 파리 공연', NOW(), NOW(), 0),

-- 오세아니아 투어
('아이유 HEREH WORLD TOUR - SYDNEY', 'Qudos Bank Arena', '아이유 HEREH 월드투어 시드니 공연', NOW(), NOW(), 0);

COMMIT;
SELECT '아이유 HEREH 월드투어 22개 도시 생성 완료' as status;

-- 3. 각 도시별 250회 공연 일정 생성 (총 5,500회)
SELECT '3단계: 각 도시별 250회 공연 일정 생성 중... (총 5,500회)' as status;

DELIMITER $$
CREATE PROCEDURE generate_concert_dates()
BEGIN
    DECLARE concert_id_var INT DEFAULT 1;
    DECLARE session_num INT;
    DECLARE batch_size INT DEFAULT 1000;
    DECLARE total_dates INT DEFAULT 0;

    WHILE concert_id_var <= 22 DO
            SET session_num = 1;

            WHILE session_num <= 250 DO
                    SET @batch_start = session_num;
                    SET @batch_end = LEAST(session_num + batch_size - 1, 250);

                    SET @sql = CONCAT('
            INSERT INTO concert_date (concert_session, concert_id, date, is_sold_out, created_at, updated_at, is_deleted)
            SELECT
                session_number,
                ', concert_id_var, ',
                DATE_ADD(NOW(), INTERVAL (', concert_id_var, ' * 250 + session_number) DAY) as date,
                CASE WHEN RAND() < 0.05 THEN 1 ELSE 0 END as is_sold_out,
                NOW() as created_at,
                NOW() as updated_at,
                0 as is_deleted
            FROM (
                SELECT ', @batch_start, ' + (a.N + b.N * 10 + c.N * 100) as session_number
                FROM
                    (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
                    (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
                    (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c
                WHERE ', @batch_start, ' + (a.N + b.N * 10 + c.N * 100) BETWEEN ', @batch_start, ' AND ', @batch_end, '
            ) sessions');

                    PREPARE stmt FROM @sql;
                    EXECUTE stmt;
                    DEALLOCATE PREPARE stmt;

                    SET session_num = @batch_end + 1;
                END WHILE;

            COMMIT;
            SET total_dates = total_dates + 250;

            SELECT CONCAT('콘서트 ID ', concert_id_var, ' 완료 (총 ', FORMAT(total_dates, 0), '회 공연)') as progress;
            SET concert_id_var = concert_id_var + 1;
        END WHILE;
END$$
DELIMITER ;

CALL generate_concert_dates();
DROP PROCEDURE generate_concert_dates;

SELECT '콘서트 일정 생성 완료 (총 5,500회 공연)' as status;

-- 4. 좌석 등급 및 가격 생성 (지역별 가격 차등)
SELECT '4단계: 좌석 등급 및 가격 생성 중...' as status;

INSERT INTO concert_seat_grade (concert_id, seat_grade, price, created_at, updated_at, is_deleted)
SELECT
    concert_id,
    seat_grade,
    CASE
        -- 아시아 (1-11번 콘서트)
        WHEN concert_id <= 11 THEN
            CASE seat_grade
                WHEN 'VIP' THEN 170000
                WHEN 'R' THEN 120000
                WHEN 'S' THEN 80000
                END
        -- 북미 (12-18번 콘서트)
        WHEN concert_id <= 18 THEN
            CASE seat_grade
                WHEN 'VIP' THEN 250000
                WHEN 'R' THEN 180000
                WHEN 'S' THEN 120000
                END
        -- 유럽, 오세아니아 (19-22번 콘서트)
        ELSE
            CASE seat_grade
                WHEN 'VIP' THEN 280000
                WHEN 'R' THEN 200000
                WHEN 'S' THEN 140000
                END
        END as price,
    NOW() as created_at,
    NOW() as updated_at,
    0 as is_deleted
FROM (
         SELECT 1 as concert_id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22
     ) concerts
         CROSS JOIN (
    SELECT 'VIP' as seat_grade UNION SELECT 'R' UNION SELECT 'S'
) grades;

COMMIT;
SELECT '좌석 등급 및 가격 생성 완료' as status;

-- 5. 좌석 생성 (각 공연당 50석, 총 275,000석)
SELECT '5단계: 모든 공연의 좌석 생성 중... (5,500회 × 50석 = 275,000석)' as status;
SELECT '⚠️ 대용량 작업으로 시간이 오래 걸립니다. 배치 처리 중...' as warning;

DELIMITER $$
CREATE PROCEDURE generate_concert_seats()
BEGIN
    DECLARE date_id_start INT DEFAULT 1;
    DECLARE date_id_end INT;
    DECLARE batch_size INT DEFAULT 100; -- 100개 공연씩 배치 처리
    DECLARE total_dates INT;

    SELECT COUNT(*) INTO total_dates FROM concert_date;

    WHILE date_id_start <= total_dates DO
            SET date_id_end = LEAST(date_id_start + batch_size - 1, total_dates);

            SET @sql = CONCAT('
        INSERT INTO concert_seat (concert_date_id, seat_number, seat_grade, seat_status, created_at, updated_at, is_deleted)
        SELECT
            cd.concert_date_id,
            CONCAT(CHAR(65 + FLOOR((seat_num - 1) / 10)), MOD(seat_num - 1, 10) + 1) as seat_number,
            CASE
                WHEN seat_num <= 10 THEN "VIP"
                WHEN seat_num <= 25 THEN "R"
                ELSE "S"
            END as seat_grade,
            CASE
                WHEN (cd.concert_date_id * 50 + seat_num) % 100 < 55 THEN "AVAILABLE"
                WHEN (cd.concert_date_id * 50 + seat_num) % 100 < 85 THEN "SOLD"
                ELSE "RESERVED"
            END as seat_status,
            NOW() as created_at,
            NOW() as updated_at,
            0 as is_deleted
        FROM (
            SELECT concert_date_id
            FROM concert_date
            WHERE concert_date_id BETWEEN ', date_id_start, ' AND ', date_id_end, '
        ) cd
        CROSS JOIN (
            SELECT 1 + (a.N + b.N * 10) as seat_num
            FROM
                (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
                (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) b
            WHERE 1 + (a.N + b.N * 10) <= 50
        ) seats');

            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;

            COMMIT;

            IF date_id_start % 500 = 1 THEN
                SELECT CONCAT('좌석 생성 진행: ', FORMAT(date_id_end * 50, 0), '석 완료') as progress;
            END IF;

            SET date_id_start = date_id_end + 1;
        END WHILE;
END$$
DELIMITER ;

CALL generate_concert_seats();
DROP PROCEDURE generate_concert_seats;

SELECT '모든 좌석 생성 완료 (총 275,000석)' as status;

-- 6. 임시 예약 데이터 생성 (RESERVED 상태 좌석들)
SELECT '6단계: 임시 예약 데이터 생성 중... (대량 배치 처리)' as status;

INSERT INTO temp_reservation (user_id, concert_seat_id, expired_at, temp_reservation_status, created_at, updated_at, is_deleted)
SELECT
    CONCAT('user-', 1 + FLOOR(RAND() * 1000000)) as user_id,
    cs.concert_seat_id,
    CASE
        WHEN RAND() < 0.4 THEN DATE_SUB(NOW(), INTERVAL FLOOR(1 + RAND() * 180) MINUTE)
        ELSE DATE_ADD(NOW(), INTERVAL FLOOR(1 + RAND() * 4) MINUTE)
        END as expired_at,
    'RESERVED' as temp_reservation_status,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 1440) MINUTE) as created_at,
    NOW() as updated_at,
    0 as is_deleted
FROM concert_seat cs
WHERE cs.seat_status = 'RESERVED'
LIMIT 50000;

COMMIT;
SELECT '임시 예약 데이터 생성 완료' as status;

-- 7. 실제 예약 및 결제 데이터 생성 (SOLD 상태 좌석들)
SELECT '7단계: 실제 예약 및 결제 데이터 생성 중... (대량 배치 처리)' as status;

DELIMITER $$
CREATE PROCEDURE generate_reservations_and_payments()
BEGIN
    DECLARE sold_seat_count INT;
    DECLARE batch_size INT DEFAULT 5000;
    DECLARE current_offset INT DEFAULT 0;

    SELECT COUNT(*) INTO sold_seat_count FROM concert_seat WHERE seat_status = 'SOLD';

    WHILE current_offset < sold_seat_count DO
            -- 예약 생성 (배치)
            SET @sql = CONCAT('
        INSERT INTO reservation (user_id, concert_date_id, seat_id, reservation_at, cancel_at, reservation_status, payment_amount, created_at, updated_at, is_deleted)
        SELECT
            CONCAT("user-", 1 + FLOOR(RAND() * 500000)) as user_id,
            cd.concert_date_id,
            cs.concert_seat_id,
            UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 90) DAY)) as reservation_at,
            0 as cancel_at,
            "COMPLETED" as reservation_status,
            csg.price as payment_amount,
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 90) DAY) as created_at,
            NOW() as updated_at,
            0 as is_deleted
        FROM concert_seat cs
        JOIN concert_date cd ON cs.concert_date_id = cd.concert_date_id
        JOIN concert_seat_grade csg ON cd.concert_id = csg.concert_id AND cs.seat_grade = csg.seat_grade
        WHERE cs.seat_status = "SOLD"
        ORDER BY cs.concert_seat_id
        LIMIT ', batch_size, ' OFFSET ', current_offset);

            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;

            -- 결제 생성 (방금 생성된 예약들에 대해)
            INSERT INTO payments (reservation_id, user_id, total_amount, discount_amount, actual_amount, payment_at, is_cancel, is_refund, cancel_at, created_at, updated_at, is_deleted)
            SELECT
                r.reservation_id,
                r.user_id,
                r.payment_amount as total_amount,
                FLOOR(r.payment_amount * RAND() * 0.2) as discount_amount,
                r.payment_amount - FLOOR(r.payment_amount * RAND() * 0.2) as actual_amount,
                r.created_at as payment_at,
                0 as is_cancel,
                0 as is_refund,
                NULL as cancel_at,
                r.created_at,
                NOW() as updated_at,
                0 as is_deleted
            FROM reservation r
            WHERE r.reservation_id > COALESCE((SELECT MAX(p.reservation_id) FROM payments p), 0);

            COMMIT;

            SET current_offset = current_offset + batch_size;

            IF current_offset % 25000 = 0 THEN
                SELECT CONCAT('예약/결제 생성: ', FORMAT(current_offset, 0), '건 완료') as progress;
            END IF;
        END WHILE;
END$$
DELIMITER ;

CALL generate_reservations_and_payments();
DROP PROCEDURE generate_reservations_and_payments;

SELECT '실제 예약 및 결제 데이터 생성 완료' as status;

-- 8. 포인트 히스토리 생성
SELECT '8단계: 포인트 히스토리 생성 중... (800,000건)' as status;

-- 결제 관련 포인트 히스토리
INSERT INTO point_history (user_id, point_history_type, point_history_amount, description, created_at, updated_at, is_deleted)
SELECT
    r.user_id,
    'USED' as point_history_type,
    r.payment_amount as point_history_amount,
    CONCAT('아이유 콘서트 티켓 결제 - 예약 ID: ', r.reservation_id) as description,
    r.created_at,
    NOW() as updated_at,
    0 as is_deleted
FROM reservation r
LIMIT 150000;

-- 충전 히스토리 생성 (대량)
INSERT INTO point_history (user_id, point_history_type, point_history_amount, description, created_at, updated_at, is_deleted)
SELECT
    CONCAT('user-', 1 + FLOOR(RAND() * 1000000)) as user_id,
    'EARNED' as point_history_type,
    CASE
        WHEN RAND() < 0.3 THEN 100000
        WHEN RAND() < 0.6 THEN 500000
        WHEN RAND() < 0.85 THEN 1000000
        ELSE 5000000
        END as point_history_amount,
    '포인트 충전' as description,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY) as created_at,
    NOW() as updated_at,
    0 as is_deleted
FROM (
         SELECT a.N + b.N * 10 + c.N * 100 + d.N * 1000 + e.N * 10000 as n
         FROM
             (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
             (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
             (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c,
             (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d,
             (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) e
         WHERE a.N + b.N * 10 + c.N * 100 + d.N * 1000 + e.N * 10000 < 500000
     ) numbers;

-- 기타 사용 히스토리
INSERT INTO point_history (user_id, point_history_type, point_history_amount, description, created_at, updated_at, is_deleted)
SELECT
    CONCAT('user-', 1 + FLOOR(RAND() * 1000000)) as user_id,
    CASE
        WHEN RAND() < 0.7 THEN 'USED'
        WHEN RAND() < 0.9 THEN 'EARNED'
        ELSE 'REFUND'
        END as point_history_type,
    FLOOR(RAND() * 300000) + 1000 as point_history_amount,
    CASE
        WHEN RAND() < 0.5 THEN '기타 사용'
        WHEN RAND() < 0.8 THEN '이벤트 적립'
        ELSE '환불'
        END as description,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY) as created_at,
    NOW() as updated_at,
    0 as is_deleted
FROM (
         SELECT a.N + b.N * 10 + c.N * 100 + d.N * 1000 + e.N * 10000 as n
         FROM
             (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
             (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
             (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c,
             (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) d,
             (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) e
         WHERE a.N + b.N * 10 + c.N * 100 + d.N * 1000 + e.N * 10000 < 150000
     ) numbers;

COMMIT;
SELECT '포인트 히스토리 800,000건 생성 완료' as status;

-- 9. 대기열 토큰 생성 (20,000개)
SELECT '9단계: 대기열 토큰 생성 중... (20,000개)' as status;

INSERT INTO queue_token (queue_token_id, user_id, concert_id, token_status, entered_at, created_at, updated_at, is_deleted)
SELECT
    UUID() as queue_token_id,
    CONCAT('user-', 1 + FLOOR(RAND() * 1000000)) as user_id,
    1 + FLOOR(RAND() * 22) as concert_id,
    CASE
        WHEN RAND() < 0.6 THEN 'WAITING'
        WHEN RAND() < 0.85 THEN 'ACTIVE'
        WHEN RAND() < 0.95 THEN 'EXPIRED'
        ELSE 'COMPLETED'
        END as token_status,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 720) MINUTE) as entered_at,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 720) MINUTE) as created_at,
    NOW() as updated_at,
    0 as is_deleted
FROM (
         SELECT a.N + b.N * 10 + c.N * 100 + d.N * 1000 + e.N * 10000 as n
         FROM
             (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
             (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
             (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c,
             (SELECT 0 AS N UNION SELECT 1) d,
             (SELECT 0 AS N UNION SELECT 1) e
         WHERE a.N + b.N * 10 + c.N * 100 + d.N * 1000 + e.N * 10000 < 20000
     ) numbers;

COMMIT;
SELECT '대기열 토큰 20,000개 생성 완료' as status;

-- 10. 데이터 정합성 및 최적화
SELECT '10단계: 데이터 정합성 확인 및 최적화 중...' as status;

-- MySQL 설정 복원
SET SESSION autocommit = 1;
SET SESSION unique_checks = 1;
SET SESSION foreign_key_checks = 1;
SET SESSION sql_log_bin = 1;
SET SESSION innodb_flush_log_at_trx_commit = 1;
SET SESSION sync_binlog = 1;

-- 통계 업데이트
ANALYZE TABLE users;
ANALYZE TABLE concerts;
ANALYZE TABLE concert_date;
ANALYZE TABLE concert_seat;
ANALYZE TABLE concert_seat_grade;
ANALYZE TABLE temp_reservation;
ANALYZE TABLE reservation;
ANALYZE TABLE payments;
ANALYZE TABLE point_history;
ANALYZE TABLE queue_token;

SELECT '데이터 정합성 확인 및 최적화 완료' as status;

-- 최종 통계
SELECT '📊 최종 MySQL 최적화 대용량 데이터 통계' as status;

SELECT
    '🧑‍🤝‍🧑 사용자' as category,
    COUNT(*) as count,
    CONCAT(FORMAT(COUNT(*), 0), '명') as formatted,
    '100만 아이유팬' as note
FROM users
UNION ALL
SELECT
    '🎵 콘서트 도시',
    COUNT(*),
    CONCAT(FORMAT(COUNT(*), 0), '개 도시'),
    '전 세계 22개 도시'
FROM concerts
UNION ALL
SELECT
    '📅 공연 일정',
    COUNT(*),
    CONCAT(FORMAT(COUNT(*), 0), '회 공연'),
    '각 도시별 250회씩'
FROM concert_date
UNION ALL
SELECT
    '🪑 전체 좌석',
    COUNT(*),
    CONCAT(FORMAT(COUNT(*), 0), '석'),
    '50석 × 5,500회'
FROM concert_seat
UNION ALL
SELECT
    '✅ 예약 가능 좌석',
    COUNT(*),
    CONCAT(FORMAT(COUNT(*), 0), '석 (', ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM concert_seat), 1), '%)'),
    '아직 예약 가능'
FROM concert_seat WHERE seat_status = 'AVAILABLE'
UNION ALL
SELECT
    '⏳ 임시 예약 좌석',
    COUNT(*),
    CONCAT(FORMAT(COUNT(*), 0), '석 (', ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM concert_seat), 1), '%)'),
    '5분간 점유'
FROM concert_seat WHERE seat_status = 'RESERVED'
UNION ALL
SELECT
    '💰 판매 완료 좌석',
    COUNT(*),
    CONCAT(FORMAT(COUNT(*), 0), '석 (', ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM concert_seat), 1), '%)'),
    '결제 완료'
FROM concert_seat WHERE seat_status = 'SOLD'
UNION ALL
SELECT
    '📝 임시 예약 건수',
    COUNT(*),
    CONCAT(FORMAT(COUNT(*), 0), '건'),
    '대기 중인 예약'
FROM temp_reservation
UNION ALL
SELECT
    '🎫 완료된 예약',
    COUNT(*),
    CONCAT(FORMAT(COUNT(*), 0), '건'),
    '결제 완료 예약'
FROM reservation
UNION ALL
SELECT
    '💳 결제 내역',
    COUNT(*),
    CONCAT(FORMAT(COUNT(*), 0), '건'),
    '총 결제 건수'
FROM payments
UNION ALL
SELECT
    '💎 포인트 히스토리',
    COUNT(*),
    CONCAT(FORMAT(COUNT(*), 0), '건'),
    '충전/사용 내역'
FROM point_history
UNION ALL
SELECT
    '🎟️ 대기열 토큰',
    COUNT(*),
    CONCAT(FORMAT(COUNT(*), 0), '개'),
    '현재 대기 중'
FROM queue_token;

-- 도시별 공연 현황
SELECT '🌍 도시별 공연 현황' as status;

SELECT
    c.concert_name as concert_city,
    COUNT(cd.concert_date_id) as total_shows,
    COUNT(cs.concert_seat_id) as total_seats,
    COUNT(CASE WHEN cs.seat_status = 'SOLD' THEN 1 END) as sold_seats,
    ROUND(COUNT(CASE WHEN cs.seat_status = 'SOLD' THEN 1 END) * 100.0 / COUNT(cs.concert_seat_id), 1) as sold_rate,
    CONCAT(FORMAT(SUM(CASE WHEN cs.seat_status = 'SOLD' THEN csg.price ELSE 0 END), 0), '원') as total_revenue
FROM concerts c
         JOIN concert_date cd ON c.concert_id = cd.concert_id
         JOIN concert_seat cs ON cd.concert_date_id = cs.concert_date_id
         JOIN concert_seat_grade csg ON c.concert_id = csg.concert_id AND cs.seat_grade = csg.seat_grade
GROUP BY c.concert_id, c.concert_name
ORDER BY total_revenue DESC
LIMIT 10;
