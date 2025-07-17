## Description

- **`콘서트 예약 서비스`**를 구현해 봅니다.
- 대기열 시스템을 구축하고, 예약 서비스는 작업가능한 유저만 수행할 수 있도록 해야합니다.
- 사용자는 좌석예약 시에 미리 충전한 잔액을 이용합니다.
- 좌석 예약 요청시에, 결제가 이루어지지 않더라도 일정 시간동안 다른 유저가 해당 좌석에 접근할 수 없도록 합니다.

## Requirements

- 아래 5가지 API 를 구현합니다.
    - 유저 토큰 발급 API
    - 예약 가능 날짜 / 좌석 API
    - 좌석 예약 요청 API
    - 잔액 충전 / 조회 API
    - 결제 API
- 각 기능 및 제약사항에 대해 단위 테스트를 반드시 하나 이상 작성하도록 합니다.
- 다수의 인스턴스로 어플리케이션이 동작하더라도 기능에 문제가 없도록 작성하도록 합니다.
- 동시성 이슈를 고려하여 구현합니다.
- 대기열 개념을 고려해 구현합니다.

## API Specs

1️⃣ **`주요` 유저 대기열 토큰 기능**

- 서비스를 이용할 토큰을 발급받는 API를 작성합니다.
- 토큰은 유저의 UUID 와 해당 유저의 대기열을 관리할 수 있는 정보 ( 대기 순서 or 잔여 시간 등 ) 를 포함합니다.
- 이후 모든 API 는 위 토큰을 이용해 대기열 검증을 통과해야 이용 가능합니다.

> 기본적으로 폴링으로 본인의 대기열을 확인한다고 가정하며, 다른 방안 또한 고려해보고 구현해 볼 수 있습니다.
> * 대기열 토큰 발급 API
> * 대기번호 조회 API


**2️⃣ `기본` 예약 가능 날짜 / 좌석 API**

- 예약가능한 날짜와 해당 날짜의 좌석을 조회하는 API 를 각각 작성합니다.
- 예약 가능한 날짜 목록을 조회할 수 있습니다.
- 날짜 정보를 입력받아 예약가능한 좌석정보를 조회할 수 있습니다.

> 좌석 정보는 1 ~ 50 까지의 좌석번호로 관리됩니다.


3️⃣ **`주요` 좌석 예약 요청 API**

- 날짜와 좌석 정보를 입력받아 좌석을 예약 처리하는 API 를 작성합니다.
- 좌석 예약과 동시에 해당 좌석은 그 유저에게 약 **5분**간 임시 배정됩니다. ( 시간은 정책에 따라 자율적으로 정의합니다. )
- 만약 배정 시간 내에 결제가 완료되지 않는다면 좌석에 대한 임시 배정은 해제되어야 한다.
- 누군가에게 점유된 동안에는 해당 좌석은 다른 사용자가 예약할 수 없어야 한다.

4️⃣ **`기본`**  **잔액 충전 / 조회 API**

- 결제에 사용될 금액을 API 를 통해 충전하는 API 를 작성합니다.
- 사용자 식별자 및 충전할 금액을 받아 잔액을 충전합니다.
- 사용자 식별자를 통해 해당 사용자의 잔액을 조회합니다.

5️⃣ **`주요` 결제 API**

- 결제 처리하고 결제 내역을 생성하는 API 를 작성합니다.
- 결제가 완료되면 해당 좌석의 소유권을 유저에게 배정하고 대기열 토큰을 만료시킵니다.

<aside>
💡 **KEY POINT**

</aside>

- 유저간 대기열을 요청 순서대로 정확하게 제공할 방법을 고민해 봅니다.
- 동시에 여러 사용자가 예약 요청을 했을 때, 좌석이 중복으로 배정 가능하지 않도록 합니다.

#### ERD

```mermaid
    erDiagram
    User {
        String UserId PK
        String UserName
        Int TotalPoint
        Int AvailablePoint
        Int UsedPoint
    }

    Concert {
        Int ConcertId PK
        String ConcertName
        String Location
        String Description
    }

    ConcertDate {
        Int ConcertDateId PK
        Int ConcertSession
        Int ConcertId
        DateTime Date
        Int TotalSeats
        Int AvailableSeats
        Boolean IsSoldOut
    }

    ConcertSeats {
        Int ConcertSeatId PK
        Int ConcertDateId 
        String SeatNumber 
        String SeatGrade
        String SeatStatus
    }

    ConcertSeatsGrade {
        Int ConcertSeatsGradeId PK
        Int ConcertId         
        String SeatGrade
        Int Price
    }

    TempReservation {
        Int TempReservationId PK
        String UserId
        Int SeatId
        DateTime ExpiredAt
        String TempReservationStatus
    }

    Reservation {
        Int ReservationId PK
        String UserId
        Int ConcertDateId
        Int SeatId
        DateTime ReservationAt
        DateTime CancleAt
        String ReservationStatus
        Int PaymentAmount
    }

    Payment {
        Int PaymentId PK
        Int ReservationId
        String UserId
        Int TotalAmount
        Int DiscountAmount
        Int ActualAmount
        DateTime PaymentAt
        Boolean IsCancel
        Boolean IsRefund
        DateTime CancleAt
    }
    
    QueueToken {
        String queueTokenId PK
        String userId
        Int ConcertId
        String Status
        DateTime enteredAt
    }

    PointHistory {
        Int HistoryId PK
        String UserId
        String Type 
        Int Amount
        Int BalanceAfter
        String Description
        DateTime CreatedAt
    }

    Concert ||--o{ ConcertDate : "has"
    ConcertDate ||--o{ ConcertSeats : "contains"
    User ||--o{ TempReservation : "holds"
    ConcertSeats ||--o| TempReservation : "held"
    User ||--o{ Reservation : "makes"
    ConcertDate ||--o{ Reservation : "includes"
    ConcertSeats ||--o| Reservation : "reserved"
    Reservation ||--o| Payment : "paid"
    ConcertSeatsGrade ||--o{ ConcertSeats : "has"
```

##### 대기열
```mermaid
sequenceDiagram
    participant User
    participant QueueController
    participant QueueService
    participant TokenValidation
    participant WebSocketServer
    participant DB

%% 콘서트 페이지 진입 → 대기열 토큰 발급
    User->>QueueController: POST /queue/token/{concertId}
    Note over User,QueueController: {userId: "user1"}
    QueueController->>QueueService: generate queue token for concert
    QueueService->>DB: save queue info (userId, concertId, position, status: WAITING, timestamp)
    DB-->>QueueService: queue saved (position: 150)
    QueueService-->>QueueController: token with queue info
    QueueController-->>User: {uuid: "550e8400-e29b-41d4-a716-446655440000", position: 200, concertId: 1 status: "WAITING"}

%% 웹소켓 연결 및 실시간 대기열 상태 수신
    User->>WebSocketServer: connect with queue token
    Note over User,WebSocketServer: ws://domain/queue?token=jwt-token
    WebSocketServer->>TokenValidation: validate queue token
    TokenValidation->>DB: check token validity & concertId
    DB-->>TokenValidation: token valid for concertId: 1
    TokenValidation-->>WebSocketServer: connection authorized
    WebSocketServer-->>User: websocket connected

%% 실시간 대기열 상태 업데이트 (서버에서 자동 푸시)
    loop live update queue status
        Note over QueueService,WebSocketServer: 다른 사용자들이 결제 완료하며 대기열 진행
        QueueService->>DB: update queue positions
        QueueService->>WebSocketServer: notify position change
        WebSocketServer->>User: {uuid: "550e8400-e29b-41d4-a716-446655440000", position: 100, concertId: 1 status: "WAITING"}

        QueueService->>WebSocketServer: notify position change
        WebSocketServer->>User: {uuid: "550e8400-e29b-41d4-a716-446655440000", position: 50, concertId: 1, status: "WAITING"}

        QueueService->>WebSocketServer: notify position change
        WebSocketServer->>User: {uuid: "550e8400-e29b-41d4-a716-446655440000", position: 10, concertId: 1, status: "WAITING"}
    end

%% 대기열 통과 - 예약 권한 활성화
    QueueService->>DB: update user status to ACTIVE
    QueueService->>WebSocketServer: notify queue passed
    WebSocketServer->>User: {uuid: "550e8400-e29b-41d4-a716-446655440000", position: 0, status: "ACTIVE", message: "예약 가능합니다!"}

    Note over User: 이제 예약 페이지 접근 가능

%% 수동 상태 조회 (만료된 경우)
    User->>QueueController: GET /queue/status
    Note over User,QueueController: Bearer token in header
    QueueController->>TokenValidation: validate queue token
    TokenValidation->>DB: check token validity for concertId
    DB-->>TokenValidation: token expired (소켓 끊김으로 인한 만료)
    TokenValidation-->>QueueController: 401 Unauthorized
    QueueController-->>User: {uuid: "550e8400-e29b-41d4-a716-446655440000", status: "EXPIRED", message: "연결 끊김으로 대기열 만료됨"}

    alt cancel queue
        User->>QueueController: DELETE /queue/token
        QueueController->>QueueService: remove from queue for concertId
        QueueService->>DB: delete queue info WHERE userId AND concertId
        DB-->>QueueService: removed
        QueueService->>WebSocketServer: notify disconnect
        WebSocketServer->>User: disconnect
        QueueService-->>QueueController: queue left
        QueueController-->>User: 200 OK
    end

    alt disconnect websocket
        Note over WebSocketServer: 연결 끊어짐 감지
        WebSocketServer->>QueueService: notify connection lost
        QueueService->>DB: update status to EXPIRED WHERE userId AND concertId
        DB-->>QueueService: queue expired
        Note over User: 대기열에서 자동 제거됨
    end
```

##### 예약 가능 날짜/좌석 조회

```mermaid
    sequenceDiagram
    participant User
    participant ConcertController
    participant TokenValidation
    participant ConcertService
    participant DB

%% 전체 콘서트 목록 조회 (대기열 토큰 불필요)
    User->>ConcertController: GET /concerts
    Note over User,ConcertController: 대기열 토큰 불필요
    ConcertController->>ConcertService: get all concerts
    ConcertService->>DB: SELECT concerts with basic info
    DB-->>ConcertService: concert list
    ConcertService-->>ConcertController: concerts info
    ConcertController-->>User: {concerts: [{id, name, location, description},...]}

%% 특정 콘서트의 예약 가능 날짜 조회 (해당 콘서트 대기열 토큰 필요)
    User->>ConcertController: GET /concerts/{concertId}/dates
    Note over User,ConcertController: 특정 콘서트 대기열 토큰 필요
    ConcertController->>TokenValidation: validate queue token for concertId
    TokenValidation->>DB: check token & queue status for specific concert
    DB-->>TokenValidation: token valid & status = 'ACTIVE' for concertId
    TokenValidation-->>ConcertController: authorized (queue passed for this concert)
    ConcertController->>ConcertService: get available dates for concertId
    ConcertService->>DB: SELECT available dates WHERE concertId
    DB-->>ConcertService: available dates for this concert
    ConcertService-->>ConcertController: concert dates
    ConcertController-->>User: {dates: [date1, date2, ...], concertInfo}

%% 특정 날짜의 좌석 조회 (해당 콘서트 대기열 토큰 필요)
    User->>ConcertController: GET /concerts/{concertId}/dates/{dateId}/seats
    Note over User,ConcertController: 콘서트별 대기열 토큰 필요
    ConcertController->>TokenValidation: validate queue token for concertId
    TokenValidation->>DB: check token & queue status for specific concert
    DB-->>TokenValidation: token valid & status = 'ACTIVE' for concertId
    TokenValidation-->>ConcertController: authorized (queue passed for this concert)
    ConcertController->>ConcertService: get available seats for concertDateId
    ConcertService->>DB: SELECT seats WHERE concertDateId AND status = 'AVAILABLE'
    DB-->>ConcertService: seats 1-50 with status
    ConcertService-->>ConcertController: seat information
    ConcertController-->>User: {seats: [{seatNumber: 1, grade: "VIP", status: "AVAILABLE"},...]}

    alt wrong concertToken
        TokenValidation->>DB: check token concertId vs requested concertId
        DB-->>TokenValidation: token for different concert
        TokenValidation-->>ConcertController: 403 Forbidden
        ConcertController-->>User: invalid concert queue token
    end

    alt still waiting
        TokenValidation->>DB: check queue status for concertId
        DB-->>TokenValidation: status = 'WAITING' for this concert
        TokenValidation-->>ConcertController: 423 Locked
        ConcertController-->>User: queue not passed for this concert
    end

    alt full seats
        ConcertService->>DB: SELECT available seats WHERE concertDateId
        DB-->>ConcertService: no available seats for this date
        ConcertService-->>ConcertController: sold out
        ConcertController-->>User: {seats: [], message: "Sold out for this date"}
    end
```

##### 좌석 예약 요청

```mermaid
    sequenceDiagram
    participant User
    participant ReservationController
    participant TokenValidation
    participant ReservationService
    participant QueueService
    participant DB
    participant SchedulerService

%% 좌석 예약 요청
    User->>ReservationController: POST /reservations
    Note over User,ReservationController: {concertDateId, seatId}
    ReservationController->>TokenValidation: validate queue token
    TokenValidation->>DB: check token & queue status
    DB-->>TokenValidation: token valid & queue active
    TokenValidation-->>ReservationController: authorized

    ReservationController->>ReservationService: create temp reservation
    ReservationService->>DB: BEGIN TRANSACTION
    ReservationService->>DB: SELECT seat FOR UPDATE
    DB-->>ReservationService: seat info (status: AVAILABLE)

    ReservationService->>DB: INSERT temp_reservation (expires_at: NOW() + 5min)
    DB-->>ReservationService: temp reservation created
    ReservationService->>DB: UPDATE seat status = 'RESERVED'
    DB-->>ReservationService: seat status updated

%% 대기열 토큰 만료 처리 추가
    ReservationService->>QueueService: expire queue token
    QueueService->>DB: UPDATE queue_token SET status = 'USED' WHERE userId AND concertId
    DB-->>QueueService: token expired
    QueueService-->>ReservationService: token expiry confirmed

    ReservationService->>DB: COMMIT TRANSACTION

    ReservationService->>SchedulerService: schedule expiry job (5min)
    SchedulerService-->>ReservationService: expiry scheduled

    ReservationService-->>ReservationController: reservation success
    ReservationController-->>User: {reservationId, expiresAt, seatInfo, message: "대기열 토큰이 만료되었습니다"}

%% 임시 예약 만료 처리 (5분 후 자동 실행)
    Note over SchedulerService,DB: 5분 후 자동 실행
    SchedulerService->>DB: BEGIN TRANSACTION
    SchedulerService->>DB: SELECT temp_reservation WHERE expired
    DB-->>SchedulerService: expired reservations
    SchedulerService->>DB: UPDATE seat status = 'AVAILABLE'
    SchedulerService->>DB: UPDATE temp_reservation status = 'EXPIRED'

%% 예약 상태 조회
    User->>ReservationController: GET /reservations/{reservationId}
    ReservationController->>TokenValidation: validate queue token
    Note over TokenValidation: 토큰이 이미 만료된 상태이므로 별도 처리 필요
    alt token expired due to reservation
        TokenValidation-->>ReservationController: token used/expired but reservation exists
        ReservationController->>DB: SELECT reservation status directly
        DB-->>ReservationController: reservation info
        ReservationController-->>User: {status, expiresAt, message: "예약으로 인해 토큰 만료됨"}
    else new token required
        TokenValidation-->>ReservationController: 401 Unauthorized
        ReservationController-->>User: new queue token required
    end

    alt already taken seat
        ReservationService->>DB: SELECT seat status
        DB-->>ReservationService: status: RESERVED or SOLD
        ReservationService-->>ReservationController: 409 Conflict
        ReservationController-->>User: seat already taken
    end

    alt multiple reservations attempt
        Note over User: 이미 토큰이 만료된 상태에서 추가 예약 시도
        ReservationController->>TokenValidation: validate queue token
        TokenValidation->>DB: check token status
        DB-->>TokenValidation: token status = 'USED'
        TokenValidation-->>ReservationController: 401 Unauthorized
        ReservationController-->>User: {message: "새로운 대기열 토큰이 필요합니다"}
    end
```

##### 잔액 충전/조회

```mermaid
    sequenceDiagram
    participant User
    participant UserController
    participant UserService
    participant DB

%% 잔액 조회
    User->>UserController: GET /users/{userId}/balance

    UserController->>UserService: get user balance
    UserService->>DB: SELECT totalPoint, availablePoint, usedPoint FROM users
    DB-->>UserService: balance info
    UserService-->>UserController: user balance
    UserController-->>User: {totalPoint: 100000, availablePoint: 50000, usedPoint: 50000}

%% 잔액 충전
    User->>UserController: POST /users/{userId}/charge
    Note over User,UserController: {amount: 50000}
    UserController->>UserService: charge balance
    UserService->>DB: BEGIN TRANSACTION
    UserService->>DB: SELECT user FOR UPDATE
    DB-->>UserService: current user info
    UserService->>DB: UPDATE users SET totalPoint = totalPoint + amount, availablePoint = availablePoint + amount
    DB-->>UserService: balance updated
    UserService->>DB: INSERT point_transaction (type: EARNED, amount)
    DB-->>UserService: transaction logged
    UserService->>DB: COMMIT TRANSACTION

    UserService-->>UserController: charge completed
    UserController-->>User: {newBalance: 100000, chargedAmount: 50000, transactionId}

%% 포인트 사용 이력 조회
    User->>UserController: GET /users/{userId}/point-history
    UserController->>UserService: get point history
    UserService->>DB: SELECT * FROM point_transaction WHERE userId ORDER BY createdAt DESC
    DB-->>UserService: transaction history
    UserService-->>UserController: point history
    UserController-->>User: {transactions: [{type, amount, balance, createdAt},...]}

    alt 잘못된 충전 금액
        UserService->>UserService: validate amount (> 0, <= MAX_CHARGE)
        UserService-->>UserController: 400 Bad Request
        UserController-->>User: invalid charge amount
    end

    alt 사용자 없음
        UserService->>DB: SELECT user
        DB-->>UserService: user not found
        UserService-->>UserController: 404 Not Found
        UserController-->>User: user not found
    end

    alt 동시 충전 요청
        UserService->>DB: SELECT user FOR UPDATE (WAIT)
        Note over DB: 락 대기 중
        DB-->>UserService: user info after wait
        UserService->>DB: UPDATE balance
        UserService-->>UserController: charge completed
        UserController-->>User: balance updated
    end
```

##### 결제

```mermaid
    sequenceDiagram
    participant User
    participant PaymentController
    participant PaymentService
    participant QueueService
    participant DB

%% 결제 처리
    User->>PaymentController: POST /payments
    Note over User,PaymentController: {reservationId, pointsToUse}
    PaymentController->>PaymentService: process payment
    PaymentService->>DB: BEGIN TRANSACTION

%% 1. 임시 예약 검증
    PaymentService->>DB: SELECT temp_reservation WHERE id AND status = 'HOLDING'
    DB-->>PaymentService: reservation info (valid, not expired)

%% 2. 사용자 잔액 확인
    PaymentService->>DB: SELECT user balance
    DB-->>PaymentService: availablePoint: 100000

%% 3. 결제 금액 계산
    PaymentService->>PaymentService: calculate payment (seatPrice - pointsUsed)
    Note over PaymentService: totalAmount: 150000, pointsUsed: 50000, actualPayment: 100000

%% 4. 잔액 차감
    PaymentService->>DB: UPDATE users SET availablePoint = availablePoint - actualPayment, usedPoint = usedPoint + pointsUsed
    DB-->>PaymentService: balance updated

%% 5. 결제 기록 생성
    PaymentService->>DB: INSERT payment (reservationId, totalAmount, pointsUsed, actualPayment)
    DB-->>PaymentService: payment record created

%% 6. 예약 확정
    PaymentService->>DB: UPDATE temp_reservation SET status = 'CONFIRMED'
    PaymentService->>DB: UPDATE seat SET status = 'SOLD'
    PaymentService->>DB: INSERT reservation (userId, seatId, status = 'COMPLETE')
    DB-->>PaymentService: reservation confirmed

%% 7. 포인트 사용 이력
    PaymentService->>DB: INSERT point_transaction (type: USED, amount: pointsUsed)
    DB-->>PaymentService: point transaction logged

    PaymentService->>DB: COMMIT TRANSACTION

%% 결제 내역 조회
    User->>PaymentController: GET /payments/{paymentId}
    PaymentController->>DB: SELECT payment details
    DB-->>PaymentController: payment info
    PaymentController-->>User: {paymentDetails, seatInfo, concertInfo}

    alt 임시 예약 만료
        PaymentService->>DB: SELECT temp_reservation
        DB-->>PaymentService: reservation expired or not found
        PaymentService->>DB: ROLLBACK TRANSACTION
        PaymentService-->>PaymentController: 410 Gone
        PaymentController-->>User: reservation expired
    end

    alt 잔액 부족
        PaymentService->>DB: SELECT user balance
        DB-->>PaymentService: insufficient balance
        PaymentService->>DB: ROLLBACK TRANSACTION
        PaymentService-->>PaymentController: 400 Bad Request
        PaymentController-->>User: insufficient balance
    end

    alt 이미 결제 완료
        PaymentService->>DB: SELECT reservation status
        DB-->>PaymentService: already paid
        PaymentService-->>PaymentController: 409 Conflict
        PaymentController-->>User: already paid
    end
```