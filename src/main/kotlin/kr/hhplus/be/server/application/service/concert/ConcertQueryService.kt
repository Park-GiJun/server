package kr.hhplus.be.server.application.service.concert

import kr.hhplus.be.server.application.dto.concert.ConcertDateWithStatsResult
import kr.hhplus.be.server.application.dto.concert.ConcertResult
import kr.hhplus.be.server.application.dto.concert.ConcertSeatWithPriceResult
import kr.hhplus.be.server.application.dto.concert.GetConcertDatesQuery
import kr.hhplus.be.server.application.dto.concert.GetConcertSeatsQuery
import kr.hhplus.be.server.application.dto.concert.PopularConcertDto
import kr.hhplus.be.server.application.mapper.ConcertMapper
import kr.hhplus.be.server.application.port.out.concert.ConcertRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertDateRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatRepository
import kr.hhplus.be.server.application.port.out.concert.ConcertSeatGradeRepository
import kr.hhplus.be.server.domain.concert.ConcertDomainService
import kr.hhplus.be.server.domain.concert.exception.ConcertNotFoundException
import kr.hhplus.be.server.application.port.`in`.concert.GetConcertDatesUseCase
import kr.hhplus.be.server.application.port.`in`.concert.GetConcertListUseCase
import kr.hhplus.be.server.application.port.`in`.concert.GetConcertSeatsUseCase
import kr.hhplus.be.server.application.port.`in`.concert.GetHotConcertUseCase
import kr.hhplus.be.server.application.port.`in`.concert.GetPopularConcertUseCase
import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
@Transactional(readOnly = true)
class ConcertQueryService(
    private val concertRepository: ConcertRepository,
    private val concertDateRepository: ConcertDateRepository,
    private val concertSeatRepository: ConcertSeatRepository,
    private val concertSeatGradeRepository: ConcertSeatGradeRepository,
    private val cacheManager: CacheManager,

    private val redisTemplate: RedisTemplate<String, Any>
) : GetConcertListUseCase, GetConcertDatesUseCase, GetConcertSeatsUseCase,
    GetPopularConcertUseCase, GetHotConcertUseCase {

    private val concertDomainService = ConcertDomainService()
    private val log = LoggerFactory.getLogger(ConcertQueryService::class.java)

    companion object {
        private const val HOT_CONCERTS_KEY = "hot:concerts"
        private const val CONCERT_NAME_CACHE = "concert:names"
    }

    override fun getConcertList(): List<ConcertResult> {
        val concerts = concertRepository.findConcertList()
        return ConcertMapper.toResults(concerts)
    }

    override fun getConcertDates(command: GetConcertDatesQuery): List<ConcertDateWithStatsResult> {
        val concertDates = concertDateRepository.findByConcertId(command.concertId)
        if (concertDates.isEmpty()) {
            throw ConcertNotFoundException(command.concertId)
        }

        increaseConcert(command.concertId)

        val results = concertDates.map { concertDate ->
            val seats = concertSeatRepository.findByConcertDateId(concertDate.concertDateId)
            val (totalSeats, availableSeats) = concertDomainService.calculateSeatStatistics(seats)
            ConcertMapper.toDateWithStatsResult(
                domain = concertDate,
                totalSeats = totalSeats,
                availableSeats = availableSeats
            )
        }
        return results
    }

    override fun getConcertSeats(command: GetConcertSeatsQuery): List<ConcertSeatWithPriceResult> {
        val concertDate = concertDateRepository.findByConcertDateId(command.concertDateId)
        concertDomainService.validateConcertDateExists(concertDate, command.concertDateId)
        val concert = concertRepository.findByConcertId(concertDate!!.concertId)
        concertDomainService.validateConcertExists(concert)
        concertDomainService.validateConcertAvailability(concertDate, concert!!.concertName)
        val seats = concertSeatRepository.findByConcertDateId(command.concertDateId)
        concertDomainService.validateSeatsExist(seats, command.concertDateId)
        val seatGrades = concertSeatGradeRepository.findByConcertId(concertDate.concertId)
        val seatGradePriceMap = concertDomainService.buildSeatPriceMap(seatGrades)
        val results = ConcertMapper.toSeatWithPriceResults(seats, seatGradePriceMap)
        return results
    }

    override fun getPopularConcert(limit: Int): List<PopularConcertDto> {
        val cacheKey = limit.toString()
        val cacheName = "popularConcerts"

        return try {
            val cache = cacheManager.getCache(cacheName)
            val cachedValue = cache?.get(cacheKey, List::class.java)
            if (cachedValue != null) {
                @Suppress("UNCHECKED_CAST")
                cachedValue as List<PopularConcertDto>
            } else {
                fetchAndCachePopularConcerts(limit, cache, cacheKey)
            }
        } catch (e: Exception) {
            fetchPopularConcertsFromDB(limit)
        }
    }

    @Cacheable("hotConcerts", key = "#limit")
    override fun getHotConcert(limit: Int): List<PopularConcertDto> {
        return try {
            fetchHotConcertsFromRedis(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Async
    override fun increaseConcert(concertId: Long) {
        try {
            redisTemplate.opsForZSet().incrementScore(HOT_CONCERTS_KEY, concertId.toString(), 1.0)
            cacheConcertNameIfNeeded(concertId)
        } catch (e: Exception) {
        }
    }

    private fun fetchAndCachePopularConcerts(
        limit: Int,
        cache: Cache?,
        cacheKey: String
    ): List<PopularConcertDto> {
        val popularConcerts = fetchPopularConcertsFromDB(limit)
        try {
            cache?.put(cacheKey, popularConcerts)
        } catch (e: Exception) {
        }
        return popularConcerts
    }

    private fun fetchPopularConcertsFromDB(limit: Int): List<PopularConcertDto> {
        return try {
            concertRepository.findByPopularConcert(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }


    private fun fetchHotConcertsFromRedis(limit: Int): List<PopularConcertDto> {
        val hotConcerts = redisTemplate.opsForZSet()
            .reverseRangeWithScores(HOT_CONCERTS_KEY, 0, (limit - 1).toLong())
            ?: emptySet()

        return hotConcerts.map { member ->
            val concertId = member.value.toString().toLong()
            val viewCount = member.score?.toLong() ?: 0L
            val concertName = getCachedConcertName(concertId)

            PopularConcertDto(
                concertId = concertId,
                concertName = concertName,
                reservedCount = viewCount
            )
        }
    }

    private fun getCachedConcertName(concertId: Long): String {
        val cached = redisTemplate.opsForHash<String, String>()
            .get(CONCERT_NAME_CACHE, concertId.toString())
        if (cached != null) return cached

        val concert = concertRepository.findByConcertId(concertId)
        val name = concert?.concertName ?: "Unknown Concert"

        redisTemplate.opsForHash<String, String>()
            .put(CONCERT_NAME_CACHE, concertId.toString(), name)
        redisTemplate.expire(CONCERT_NAME_CACHE, Duration.ofHours(1))

        return name
    }

    private fun cacheConcertNameIfNeeded(concertId: Long) {
        val exists = redisTemplate.opsForHash<String, String>()
            .hasKey(CONCERT_NAME_CACHE, concertId.toString())

        if (!exists) {
            getCachedConcertName(concertId)
        }
    }
}