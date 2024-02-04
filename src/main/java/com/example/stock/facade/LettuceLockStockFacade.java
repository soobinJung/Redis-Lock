package com.example.stock.facade;

import com.example.stock.repository.RedisLockRepository;
import com.example.stock.service.StockService;
import org.springframework.stereotype.Component;

/**
 *
 * 장점
 * - 구현이 간단하다
 *
 * 단점
 * - 스핀 락 방식으로 레디스 서버에 부하를 줄 수 있음
 * - 레디스 서버에 장애가 발생하면 락을 해제하지 못할 수 있음
 */
@Component
public class LettuceLockStockFacade {

    private RedisLockRepository redisLockRepository;
    private StockService stockService;

    public LettuceLockStockFacade(RedisLockRepository redisLockRepository, StockService stockService) {
        this.redisLockRepository = redisLockRepository;
        this.stockService = stockService;
    }

    public void decrease(Long id, Long quantity) throws InterruptedException {

        /** Lock 획득 시도 **/
        while (!redisLockRepository.lock(id)){
            Thread.sleep(100);
        }

        try {
            /** Lock 획득 상태에서 로직 수행 **/
            stockService.decrease(id, quantity);
        } finally {

            /** Lock 해제 **/
            redisLockRepository.unlock(id);
        }
    }

}
