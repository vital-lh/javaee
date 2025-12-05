//School of Informatics Xiamen University, GPL-3.0 license
package cn.edu.xmu.javaee.productdemoredis.dao;

import cn.edu.xmu.javaee.core.util.CloneFactory;
import cn.edu.xmu.javaee.productdemoredis.dao.bo.OnSale;
import cn.edu.xmu.javaee.productdemoredis.mapper.OnSalePoMapper;
import cn.edu.xmu.javaee.productdemoredis.mapper.po.OnSalePo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Repository
@Slf4j
@RequiredArgsConstructor
public class OnSaleDao {

    private final OnSalePoMapper onSalePoMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    // Redis Key前缀
    private static final String ONSALE_KEY_PREFIX = "onsale:product:";

    /**
     * 获得货品的最近的价格和库存（缓存优先）
     * @param productId 货品对象
     * @return 规格对象
     */
    public List<OnSale> getLatestOnSale(Long productId) throws DataAccessException {
        String key = ONSALE_KEY_PREFIX + productId;
        // 1. 先查Redis缓存
        List<OnSale> onSaleList = (List<OnSale>) redisTemplate.opsForValue().get(key);
        if (onSaleList != null) {
            log.debug("getLatestOnSale from Redis: productId={}, list={}", productId, onSaleList);
            return onSaleList;
        }

        // 2. 缓存未命中，查数据库
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "endTime"));
        List<OnSalePo> onsalePoList = onSalePoMapper.findByProductIdEqualsAndBeginTimeBeforeAndEndTimeAfter(productId, now, now, pageable);
        onSaleList = onsalePoList.stream().map(po -> CloneFactory.copy(new OnSale(), po)).collect(Collectors.toList());

        // 3. 写入Redis，设置过期时间30分钟
        if (!onSaleList.isEmpty()) {
            redisTemplate.opsForValue().set(key, onSaleList, 30, TimeUnit.MINUTES);
            log.debug("getLatestOnSale write to Redis: productId={}", productId);
        }
        return onSaleList;
    }
}