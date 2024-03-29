# Redis Lock
Redis는 주로 인메모리 데이터 구조 저장소로 사용되며, 캐싱, 메시징 큐, 그리고 세션 스토어와 같은 다양한 용도로 활용됩니다. Redis는 또한 분산 락을 구현하는 데 사용될 수 있는 기능을 제공하여, **분산 시스템 내에서 데이터의 일관성과 무결성**을 유지하도록 돕습니다. Redis를 사용한 락은 주로 동시성 제어와 데이터의 안전한 수정을 보장하기 위해 사용됩니다. Redis에서는 주로 두 가지 방식의 락을 구현할 수 있습니다: Redlock 알고리즘을 사용한 분산 락과 단일 인스턴스 락입니다.

## Redis Lock 종류
Lettuce Lock과 Redisson Lock은 Java 환경에서 Redis를 사용하여 분산 락을 구현하기 위한 두 가지 인기 있는 라이브러리에 기반한 락 메커니즘입니다. 이들은 Redis의 기능을 활용하여 다중 인스턴스 환경에서 데이터의 일관성과 무결성을 유지하는 동시에, 여러 애플리케이션 인스턴스 간의 동시성을 관리합니다. 각각의 라이브러리는 다른 API와 기능 세트를 제공하지만, 궁극적으로는 Redis를 사용하여 분산 시스템 내에서 락을 구현하고 관리하는 것을 목표로 합니다.

### Lettuce Lock 
Lettuce는 비동기 이벤트 기반의 Redis 클라이언트로, Netty를 기반으로 구축되어 있습니다. 이는 고성능이며, 여러 Redis 커맨드를 동시에 실행할 수 있는 능력을 제공합니다. Lettuce Lock은 Lettuce 클라이언트를 사용하여 구현됩니다. 이 방식은 SETNX 커맨드 또는 Redis 2.6.12 버전 이후에 도입된 SET 커맨드의 확장 옵션을 사용하여 락을 구현할 수 있습니다. Lettuce를 사용한 락 구현은 다음과 같은 기능을 포함할 수 있습니다:

##### 🔊 락 재시도
락 획득 시도가 실패할 경우, 설정된 시간 간격으로 재시도할 수 있습니다.

##### 🔊 락 타임아웃
락이 자동으로 해제되도록 타임아웃을 설정할 수 있습니다. 이는 데드락을 방지하는 데 도움이 됩니다.

##### 🔊 락 갱신
락의 유지 시간을 연장할 수 있는 기능입니다.

```
@Component
public class RedisLockRepository {

    private RedisTemplate<String, String> redisTemplate;

    public RedisLockRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Boolean lock(Long key){
        return redisTemplate
                .opsForValue()
                .setIfAbsent(generateKey(key), "lock");
    }

    public void unlock(Long key){
        redisTemplate.delete(generateKey(key));
    }

    public String generateKey(Long key){
        return key.toString();
    }
}
```

```
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
```

### Redisson Lock
Redisson은 Redis를 위한 또 다른 Java 클라이언트이며, 높은 수준의 추상화를 제공합니다. 이는 객체 매핑, 분산 데이터 구조, 분산 서비스 및 유틸리티를 포함한 광범위한 기능을 제공합니다. Redisson Lock은 Redisson의 분산 락 구현체로, 더 복잡한 분산 시스템에서도 사용할 수 있도록 설계되었습니다. Redisson은 Redlock 알고리즘을 구현하여 분산 락을 제공하며, 다음과 같은 특징을 가집니다:

##### 🔊 자동 락 갱신
Redisson은 설정된 락 유지 시간이 만료되기 전에 자동으로 락을 갱신할 수 있는 기능을 제공합니다. 이는 락을 유지하는 작업이 오래 걸리는 경우 유용합니다.

##### 🔊 블로킹 및 논블로킹 락
Redisson은 블로킹 락과 논블로킹 락 모두를 지원하여, 락을 획득할 때까지 기다리거나 즉시 반환할 수 있습니다.

##### 🔊 공정성 보장
Redisson의 공정성 보장 락(Fair Lock)은 락을 요청하는 순서대로 획득할 수 있도록 합니다.

```
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
```
  
