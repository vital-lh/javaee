//School of Informatics Xiamen University, GPL-3.0 license
package cn.edu.xmu.javaee.productdemoredis.dao;

import cn.edu.xmu.javaee.core.util.CloneFactory;
import cn.edu.xmu.javaee.productdemoredis.dao.bo.OnSale;
import cn.edu.xmu.javaee.productdemoredis.mapper.OnSalePoMapper;
import cn.edu.xmu.javaee.productdemoredis.mapper.po.OnSalePo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Repository
@Slf4j
@RequiredArgsConstructor
public class OnSaleDao {

    private final OnSalePoMapper onSalePoMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // 缓存Key前缀
    private static final String ONSALE_CACHE_KEY_PREFIX = "onsale:";
    private static final String PRODUCT_ONSALE_RELATION_KEY_PREFIX = "product_onsale:";
    private static final String ONSALE_ACTIVE_LIST_KEY = "onsale:active:list";

    /**
     * 获得货品的最近的价格和库存 - 带完整缓存
     * @param productId 货品对象
     * @return 规格对象
     */
    public List<OnSale> getLatestOnSale(Long productId) throws DataAccessException {
        String relationKey = PRODUCT_ONSALE_RELATION_KEY_PREFIX + productId;

        // 1. 先尝试从关系缓存获取
        List<Object> cachedOnSaleIds = redisTemplate.opsForList().range(relationKey, 0, -1);

        if (cachedOnSaleIds != null && !cachedOnSaleIds.isEmpty()) {
            log.debug("Cache HIT for onsale relation of product: {}", productId);

            // 排除空值标记
            if (cachedOnSaleIds.size() == 1 && "empty".equals(cachedOnSaleIds.get(0))) {
                return Collections.emptyList();
            }

            // 批量获取OnSale对象缓存
            List<String> onSaleKeys = cachedOnSaleIds.stream()
                    .map(id -> ONSALE_CACHE_KEY_PREFIX + id)
                    .collect(Collectors.toList());

            List<Object> cachedOnSales = redisTemplate.opsForValue().multiGet(onSaleKeys);

            return cachedOnSales.stream()
                    .filter(Objects::nonNull)
                    .map(obj -> (OnSale) obj)
                    .collect(Collectors.toList());
        }

        log.debug("Cache MISS for onsale relation of product: {}", productId);

        // 2. 缓存未命中，查询数据库
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "endTime"));

        // 使用现有的方法，这个方法在您的代码中是存在的
        List<OnSalePo> onsalePoList = onSalePoMapper
                .findByProductIdEqualsAndBeginTimeBeforeAndEndTimeAfter(
                        productId, now, now, pageable);

        if (onsalePoList.isEmpty()) {
            // 防止缓存穿透：缓存空结果
            redisTemplate.opsForList().rightPush(relationKey, "empty");
            redisTemplate.expire(relationKey, 5, TimeUnit.MINUTES);
            return Collections.emptyList();
        }

        // 3. 构建对象缓存和关系缓存
        List<OnSale> result = new ArrayList<>();
        List<Long> onSaleIds = new ArrayList<>();

        for (OnSalePo po : onsalePoList) {
            OnSale onSale = CloneFactory.copy(new OnSale(), po);
            result.add(onSale);
            onSaleIds.add(po.getId());

            // 缓存单个OnSale对象
            String onSaleKey = ONSALE_CACHE_KEY_PREFIX + po.getId();
            redisTemplate.opsForValue().set(onSaleKey, onSale,
                    15 + (int)(Math.random() * 10), TimeUnit.MINUTES);
        }

        // 缓存关系列表
        redisTemplate.opsForList().rightPushAll(relationKey, onSaleIds.toArray());
        redisTemplate.expire(relationKey, 10, TimeUnit.MINUTES);

        return result;
    }

    /**
     * 获取活跃的促销列表（用于首页展示等）
     * 注意：此方法需要 OnSalePoMapper 中有对应的方法支持
     * 如果不存在 findByBeginTimeBeforeAndEndTimeAfter 方法，请注释掉此方法
     * 或者根据您的实际需求实现
     */
    /*
    public List<OnSale> getActiveOnSales(int limit) throws DataAccessException {
        // 1. 尝试从缓存获取活跃列表
        List<Object> cachedIds = redisTemplate.opsForList()
            .range(ONSALE_ACTIVE_LIST_KEY, 0, limit - 1);

        if (cachedIds != null && !cachedIds.isEmpty()) {
            log.debug("Cache HIT for active onsale list");

            // 批量获取对象
            List<String> keys = cachedIds.stream()
                .map(id -> ONSALE_CACHE_KEY_PREFIX + id)
                .collect(Collectors.toList());

            List<Object> cachedOnSales = redisTemplate.opsForValue().multiGet(keys);

            return cachedOnSales.stream()
                .filter(Objects::nonNull)
                .map(obj -> (OnSale) obj)
                .collect(Collectors.toList());
        }

        log.debug("Cache MISS for active onsale list");

        // 2. 缓存未命中，查询数据库
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, limit,
            Sort.by(Sort.Direction.DESC, "endTime"));

        // 这个方法可能不存在，如果不存在请注释掉这部分代码
        // 或者替换为实际可用的方法
        List<OnSalePo> onsalePoList = onSalePoMapper
            .findByBeginTimeBeforeAndEndTimeAfter(now, now, pageable);

        List<OnSale> result = onsalePoList.stream()
            .map(po -> CloneFactory.copy(new OnSale(), po))
            .collect(Collectors.toList());

        // 3. 更新缓存
        updateActiveOnSaleList(result);

        return result;
    }
    */

    /**
     * 根据ID获取OnSale对象 - 带缓存
     * @param id OnSale ID
     * @return OnSale对象
     */
    public OnSale findById(Long id) throws DataAccessException {
        String cacheKey = ONSALE_CACHE_KEY_PREFIX + id;

        // 1. 先尝试从缓存获取
        OnSale cachedOnSale = (OnSale) redisTemplate.opsForValue().get(cacheKey);
        if (cachedOnSale != null) {
            log.debug("Cache HIT for onsale: {}", id);
            return cachedOnSale;
        }

        log.debug("Cache MISS for onsale: {}", id);

        // 2. 缓存未命中，查询数据库
        OnSalePo onSalePo = onSalePoMapper.findById(id)
                .orElseThrow(() -> new DataAccessException("OnSale not found: " + id) {});

        OnSale onSale = CloneFactory.copy(new OnSale(), onSalePo);

        // 3. 写入缓存
        redisTemplate.opsForValue().set(cacheKey, onSale,
                20 + (int)(Math.random() * 10), TimeUnit.MINUTES);

        return onSale;
    }

    /**
     * 更新活跃促销列表缓存
     * @param activeOnSales 活跃促销列表
     */
    private void updateActiveOnSaleList(List<OnSale> activeOnSales) {
        if (activeOnSales.isEmpty()) {
            return;
        }

        // 提取ID列表
        List<Long> ids = activeOnSales.stream()
                .map(OnSale::getId)
                .collect(Collectors.toList());

        // 更新缓存
        redisTemplate.delete(ONSALE_ACTIVE_LIST_KEY);
        redisTemplate.opsForList().rightPushAll(ONSALE_ACTIVE_LIST_KEY, ids.toArray());
        redisTemplate.expire(ONSALE_ACTIVE_LIST_KEY, 5, TimeUnit.MINUTES);

        log.debug("Updated active onsale list cache with {} items", ids.size());
    }

    /**
     * 清理OnSale相关缓存
     * @param onSaleId OnSale ID
     * @param productId 关联的商品ID（可为null）
     */
    public void clearOnSaleCache(Long onSaleId, Long productId) {
        // 清理对象缓存
        String onSaleKey = ONSALE_CACHE_KEY_PREFIX + onSaleId;

        // 清理活跃列表缓存
        String activeListKey = ONSALE_ACTIVE_LIST_KEY;

        List<String> keysToDelete = new ArrayList<>();
        keysToDelete.add(onSaleKey);
        keysToDelete.add(activeListKey);

        // 如果知道商品ID，清理关系缓存
        if (productId != null) {
            String relationKey = PRODUCT_ONSALE_RELATION_KEY_PREFIX + productId;
            keysToDelete.add(relationKey);
        }

        redisTemplate.delete(keysToDelete);
        log.debug("Cleared onsale cache for id: {}", onSaleId);
    }
}