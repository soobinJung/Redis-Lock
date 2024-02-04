package com.example.stock.facade;

import com.example.stock.service.StockService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 장점
 *    - pub / sub 기반 락을 제공
 *
 * 단점
 *    - 구현이 복잡하다.
 *    - 별도 라이브러리를 사용해야한다.
 */
@Component
public class RedissonLockStockFacade {

    private RedissonClient redissonClient;
    private StockService stockService;

    public RedissonLockStockFacade(RedissonClient redissonClient, StockService stockService) {
        this.redissonClient = redissonClient;
        this.stockService = stockService;
    }

    public void decrease(Long id, Long quantity) {
        RLock lock = redissonClient.getLock(id.toString());
        try {
            boolean available = lock.tryLock(10, 1, TimeUnit.SECONDS);

            if (!available) {
                System.out.println("Lock 획득 실패");
                return;
            }

             stockService.decrease(id, quantity);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}
