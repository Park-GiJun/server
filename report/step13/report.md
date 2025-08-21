# Redis 적용 보고서

## 구현 내역

### 1. 대기열 시스템 구현

#### 1.1 Redis 자료구조 조합 설계
**Queue System = Hash + String + SortedSet**

| 자료구조      | 키 패턴                              | 용도                | 선택 이유            |
|-----------|-----------------------------------|-------------------|------------------|
| Hash      | `queue:token:{tokenId}`           | 토큰 상세정보 저장        | 복잡한 객체 저장에 최적    |
| String    | `queue:user:{userId}:{concertId}` | userId→tokenId 매핑 | O(1) 빠른 조회       |
| SortedSet | `queue:waiting:{concertId}`       | 대기열 순서 관리         | 순서 보장 + O(log n) |
| SortedSet | `queue:active:{concertId}`        | 활성 사용자 관리         | 개별 TTL 구현        |

#### 1.2 SortedSet을 선택한 핵심 이유

**Hash의 TTL 한계:**
```kotlin
// Hash는 전체 키에만 TTL 설정 가능
redisTemplate.opsForHash().put("queue:active:1", "user1", data)
redisTemplate.opsForHash().put("queue:active:1", "user2", data)
redisTemplate.expire("queue:active:1", Duration.ofMinutes(30)) // 전체에만 적용

// ❌ 개별 field별 다른 TTL 설정 불가능
```

**SortedSet으로 개별 TTL 해결:**
```kotlin
// Score를 만료시간으로 활용
fun activateWaitingUsers(concertId: Long, count: Int) {
    val currentTime = System.currentTimeMillis() / 1000
    val expiryTime = currentTime + 1800 // 30분 후
    
    usersToActivate.forEach { userId ->
        // 대기열에서 제거
        redisTemplate.opsForZSet().remove("queue:waiting:$concertId", userId)
        // 활성 큐에 추가 (Score = 만료시간)
        redisTemplate.opsForZSet().add("queue:active:$concertId", userId, expiryTime.toDouble())
    }
}

// 만료된 사용자만 정확히 제거
fun cleanupExpiredActiveTokens(concertId: Long) {
    val currentTime = System.currentTimeMillis() / 1000
    redisTemplate.opsForZSet().removeRangeByScore(
        "queue:active:$concertId",
        Double.NEGATIVE_INFINITY,
        currentTime.toDouble()
    )
}
```

#### 1.3 대기열 처리 프로세스
```kotlin
// 1. 대기열 진입
fun addToWaitingQueue(token: QueueToken): Long {
    val queueKey = "queue:waiting:${token.concertId}"
    val timestamp = token.enteredAt.toEpochMilli().toDouble()
    
    // Score = 진입 시간 (FIFO 보장)
    redisTemplate.opsForZSet().add(queueKey, token.userId, timestamp)
    return redisTemplate.opsForZSet().rank(queueKey, userId) ?: -1L
}

// 2. 순위 조회 (O(log n))
fun getWaitingPosition(concertId: Long, userId: String): Long {
    val queueKey = "queue:waiting:$concertId"
    return redisTemplate.opsForZSet().rank(queueKey, userId) ?: -1L
}

// 3. 배치 활성화 (스케줄러 3초마다)
@Scheduled(fixedDelay = 3000)
fun processQueueActivation() {
    val tokensToActivate = calculateTokensToActivate(currentActiveCount)
    if (tokensToActivate > 0) {
        activateWaitingUsers(concertId, tokensToActivate)
    }
}
```

### 2. 임시 예약 (TempReservation) - DB 유지 결정

#### 2.1 Redis 적용 검토
좌석 선택 후 5분간 점유라는 요구사항이 Redis TTL과 완벽하게 맞아 검토했습니다.

#### 2.2 장단점 분석

**Redis 적용시 예상 구현:**
```kotlin
// Redis만 사용한다면?
fun tempReservationWithRedis(command: TempReservationCommand) {
    redisTemplate.opsForValue().set(
        "temp:seat:${seatId}", 
        TempReservation(userId, seatId, ...),
        Duration.ofMinutes(5)  // 5분 자동 만료
    )
}
```

**장점:**
- TTL로 5분 자동 만료 처리
- DB 부하 감소
- 빠른 조회 성능

**단점 및 리스크:**
- **데이터 유실**: Redis 장애시 예약 정보 손실
- **트랜잭션 보장 어려움**: 임시예약→결제→확정의 원자성 깨짐
- **이력 추적 불가**: 감사(Audit) 로그

#### 2.3 최종 결정: DB + 분산락(기존 방식 유지)
```kotlin
@Transactional
@DistributedLock(
    type = DistributedLockType.TEMP_RESERVATION_SEAT,
    key = "'lock:temp_reservation:seat:' + #command.concertSeatId",
    waitTime = 0L,  // 즉시 실패
    leaseTime = 10L
)
override fun tempReservation(command: TempReservationCommand): TempReservationResult {
    // 1. 토큰 검증
    val tokenResult = validateTokenUseCase.validateActiveToken(...)
    
    // 2. 좌석 조회 및 검증
    val seat = concertSeatRepository.findByConcertSeatId(command.concertSeatId)
    
    // 3. DB에 임시 예약 저장 (영속성 보장)
    val tempReservation = reservationDomainService.createTempReservation(
        command.userId, 
        command.concertSeatId
    )
    tempReservationRepository.save(tempReservation)
    
    // 4. 좌석 상태 변경 (트랜잭션 보장)
    concertSeatRepository.save(seat.reserve())
    
    return result
}
```

**DB 선택 이유:**
- **데이터 무결성**: 결제 관련 핵심 데이터는 유실 방지 필수
- **트랜잭션 일관성**: 상태 전이 안정성 확보
- **비즈니스 요구사항**: 예약 이력

### 3. Popular Concert vs Hot Concert 캐싱 전략

#### 3.1 두 가지 인기 콘서트 개념 설계

**Popular Concert (실제 예약 기반):**
- **정의**: 최근 5분간 실제 예약이 많은 콘서트
- **목적**: 실시간 예약 트렌드 반영
- **구현**: DB 쿼리 + Redis 캐싱

**Hot Concert (조회수 기반):**
- **정의**: 조회수/검색량 기반 인기 콘서트
- **목적**: 사용자 관심도 측정
- **구현**: Redis SortedSet으로 실시간 카운팅

#### 3.2 Popular Concert 구현 (예약 기반)

**캐시 설정:**
```kotlin
@Configuration
class CacheConfig {
    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): CacheManager {
        val cacheConfigurations = mapOf(
            "popularConcerts" to RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(1))  // 1분 TTL
                .disableCachingNullValues()
        )
        
        return RedisCacheManager.builder(redisConnectionFactory)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }
}
```

**서비스 구현:**
```kotlin
override fun getPopularConcert(limit: Int): List<PopularConcertDto> {
    return try {
        // 1. 캐시 조회
        val cache = cacheManager.getCache("popularConcerts")
        cache?.get(limit.toString(), List::class.java)
            ?: fetchAndCachePopularConcerts(limit)
    } catch (e: Exception) {
        // 2. Redis 장애시 DB 직접 조회 (Fallback)
        fetchPopularConcertsFromDB(limit)
    }
}

// DB 쿼리 (최근 5분간 예약 기준)
@Query("""
    SELECT 
        c.concert_id AS concertId,
        c.concert_name AS concertName,
        COUNT(*) AS reservedCount
    FROM concerts c
    JOIN concert_date cd ON c.concert_id = cd.concert_id
    JOIN concert_seat cs ON cd.concert_date_id = cs.concert_date_id
    WHERE cs.created_at >= NOW() - INTERVAL 5 MINUTE
      AND cs.seat_status = 'RESERVED'
    GROUP BY c.concert_id, c.concert_name
    ORDER BY reservedCount DESC
    LIMIT :limit
""")
fun findPopularConcertsLast5Minutes(limit: Int): List<PopularConcertProjection>
```

#### 3.3 Hot Concert 구현 (조회수 기반)

**Redis SortedSet 활용 구현:**
```kotlin
@Service
class ConcertQueryService : GetHotConcertUseCase {
    
    // 조회수 증가
    override fun increaseConcert(concertId: Long) {
        val key = "hot:concerts:${LocalDate.now()}"
        
        // ZINCRBY: 조회수 1 증가
        redisTemplate.opsForZSet().incrementScore(key, concertId.toString(), 1.0)
        
        // TTL: 일별 집계 (24시간)
        redisTemplate.expire(key, Duration.ofDays(1))
        
        log.debug("조회수 증가: concertId=$concertId")
    }
    
    // Hot Concert 조회
    override fun getHotConcert(limit: Int): List<PopularConcertDto> {
        val key = "hot:concerts:${LocalDate.now()}"
        
        // ZREVRANGE: Score(조회수) 높은 순으로 상위 N개 조회
        val hotConcerts = redisTemplate.opsForZSet()
            .reverseRangeWithScores(key, 0, limit.toLong() - 1)
            ?.mapNotNull { tuple ->
                val concertId = tuple.value?.toString()?.toLongOrNull()
                val viewCount = tuple.score?.toLong() ?: 0L
                
                concertId?.let { id ->
                    // Concert 정보 조회 (캐시 또는 DB)
                    concertRepository.findByConcertId(id)?.let { concert ->
                        PopularConcertDto(
                            concertId = concert.concertId,
                            concertName = concert.concertName,
                            reservedCount = viewCount  // 조회수를 reservedCount 필드에 매핑
                        )
                    }
                }
            } ?: emptyList()
        
        log.info("Hot Concert 조회: 요청=$limit, 결과=${hotConcerts.size}")
        return hotConcerts
    }
}
```

#### 3.4 Popular vs Hot 비교

| 구분               | Popular Concert     | Hot Concert     |
|------------------|---------------------|-----------------|
| **기준**           | 실제 예약 수             | 조회/검색 수         |
| **데이터 소스**       | DB (seat_status)    | Redis (실시간 카운팅) |
| **집계 기간**        | 최근 5분               | 실시간             |
| **캐싱 전략**        | Spring Cache (1분)   | SortedSet (실시간) |
| **정확도**          | 100% (DB 기반)        | 근사치 (메모리 기반)    |
| **용도**           | 실제 인기도              | 관심도/트렌드         |
| **API Endpoint** | `/concerts/popular` | `/concerts/hot` |


## 📊 기술적 의사결정

### 시간 복잡도 비교

| 작업     | DB (기존)    | Redis SortedSet | 개선 |
|--------|------------|-----------------|----|
| 대기열 삽입 | O(1) + 인덱스 | O(log n)        | ✅  |
| 순위 조회  | O(n)       | O(log n)        | ✅  |
| 범위 조회  | O(n)       | O(log n + m)    | ✅  |
| 인기 콘서트 | 복잡한 JOIN   | 캐시 히트시 O(1)     | ✅  |
| 조회수 증가 | INSERT 쿼리  | O(log n)        | ✅  |

### TTL 정책
```kotlin
// 대기열 토큰 TTL
fun calculateTTL(status: QueueTokenStatus): Duration {
    return when (status) {
        QueueTokenStatus.WAITING -> Duration.ofHours(24)    // 대기: 24시간
        QueueTokenStatus.ACTIVE -> Duration.ofMinutes(30)   // 활성: 30분
        QueueTokenStatus.COMPLETED -> Duration.ofMinutes(5) // 완료: 5분
        QueueTokenStatus.EXPIRED -> Duration.ofMinutes(1)   // 만료: 1분
    }
}

// 캐시 TTL
val cacheConfigurations = mapOf(
    "popularConcerts" to Duration.ofMinutes(1),  // 인기 콘서트: 1분
    "hotConcerts" to Duration.ofDays(1)         // Hot 콘서트: 24시간
)
```

## 💡 핵심 교훈

### 잘한 점
- **SortedSet Score 활용**: 대기열(진입시간)과 활성큐(만료시간)를 Score로 해결
- **적재적소 저장소 선택**: 휘발성 데이터는 Redis, 핵심 데이터는 DB
- **캐싱 전략 차별화**: Popular(실제 예약)와 Hot(조회수) 구분 구현
- **안정성 우선**: 임시예약은 DB로 데이터 무결성 확보
- **두 가지 인기도 측정**: 실제 예약과 관심도를 별도로 측정

### 아쉬운 점
- 초기 설계시 캐싱 전략 미고려
- 메모리 관리 정책 구체화 필요
- 조회수 증가 로직의 중복 호출 방지 필요

### 배운 점
- Redis는 단순 캐시가 아닌 강력한 자료구조 저장소
- Hash의 TTL 한계를 SortedSet Score로 극복 가능
- 인기도 측정은 목적에 따라 다른 전략 필요 (예약 vs 조회)
- 모든 것을 Redis로 해결하지 말고 데이터 특성 고려
- 결제 관련 핵심 데이터는 반드시 DB에서 관리

## 🎯 결론

Redis 도입으로 대기열 시스템과 인기 콘서트 조회 성능을 크게 개선했습니다.

**핵심 의사결정:**
1. **대기열**: SortedSet으로 순서 보장과 개별 TTL 동시 해결
2. **임시예약**: 안정성을 위해 DB + 분산락 유지 (Redis 미적용)
3. **Popular Concert**: 실제 예약 기반
4. **Hot Concert**: 조회수 기반 실시간 집계로 사용자 관심도 측정 구현

특히 SortedSet의 Score를 timestamp/expiry time으로 활용한 것이 가장 중요한 기술적 결정이었으며, 이를 통해 Hash의 개별 TTL 한계를 극복할 수 있었습니다.

또한 인기 콘서트를 Popular(예약)과 Hot(조회) 두 가지로 모두 구현하여, 실제 예약 트렌드와 사용자 관심도를 별도로 측정할 수 있게 되었습니다.