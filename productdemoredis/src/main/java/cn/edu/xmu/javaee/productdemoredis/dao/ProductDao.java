//School of Informatics Xiamen University, GPL-3.0 license
package cn.edu.xmu.javaee.productdemoredis.dao;

import cn.edu.xmu.javaee.core.bean.RequestVariables;
import cn.edu.xmu.javaee.core.exception.BusinessException;
import cn.edu.xmu.javaee.core.model.ReturnNo;
import cn.edu.xmu.javaee.core.model.UserToken;
import cn.edu.xmu.javaee.core.util.JacksonUtil;
import cn.edu.xmu.javaee.core.util.CloneFactory;
import cn.edu.xmu.javaee.productdemoredis.dao.bo.OnSale;
import cn.edu.xmu.javaee.productdemoredis.dao.bo.Product;
import cn.edu.xmu.javaee.productdemoredis.mapper.GoodsPoMapper;
import cn.edu.xmu.javaee.productdemoredis.mapper.ProductPoMapper;
import cn.edu.xmu.javaee.productdemoredis.mapper.po.GoodsPo;
import cn.edu.xmu.javaee.productdemoredis.mapper.po.ProductPo;
import cn.edu.xmu.javaee.productdemoredis.dao.OnSaleDao;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static cn.edu.xmu.javaee.core.model.Constants.PLATFORM;

/**
 * @author Ming Qiu
 **/
@Repository
@Slf4j
public class ProductDao {

    private final ProductPoMapper productPoMapper;
    private final OnSaleDao onSaleDao;
    private final GoodsPoMapper goodsPoMapper;

    private RequestVariables requestVariables;

    private final RedisTemplate<String, Object> redisTemplate;

    // 缓存Key前缀定义
    private static final String PRODUCT_CACHE_KEY_PREFIX = "product:";
    private static final String ONSALE_CACHE_KEY_PREFIX = "onsale:";
    private static final String PRODUCT_ONSALE_RELATION_KEY_PREFIX = "product_onsale:";
    private static final String PRODUCT_RELATION_KEY_PREFIX = "product_relate:";

    // 构造函数
    @Autowired
    public ProductDao(ProductPoMapper productPoMapper,
                      OnSaleDao onSaleDao,
                      GoodsPoMapper goodsPoMapper,
                      RedisTemplate<String, Object> redisTemplate) {
        this.productPoMapper = productPoMapper;
        this.onSaleDao = onSaleDao;
        this.goodsPoMapper = goodsPoMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 用名称寻找Product对象
     *
     * @param name 名称
     * @return Product对象列表，带关联的Product返回
     */
    public List<Product> retrieveSimpleProductByName(Long shopId, String name) throws BusinessException {
        List<Product> productList = new ArrayList<>();
        List<ProductPo> productPoList;
        Pageable pageable = PageRequest.of(1, 100);
        if (PLATFORM.equals(shopId)) {
            productPoList = this.productPoMapper.findByName(name, pageable);
        } else {
            productPoList = this.productPoMapper.findByShopIdAndName(shopId, name, pageable);
        }
        for (ProductPo po : productPoList) {
            Product product = CloneFactory.copy(new Product(), po);
            productList.add(product);
        }
        log.debug("retrieveSimpleProductByName: productList = {}", productList);
        return productList;
    }

    /**
     * 用id对象找Product对象 - 带缓存
     *
     * @param shopId 商铺id
     * @param productId 产品id
     * @return Product对象，不关联的Product
     */
    public Product findSimpleProductById(Long shopId, Long productId) throws BusinessException {
        String cacheKey = PRODUCT_CACHE_KEY_PREFIX + productId;

        // 1. 先尝试从缓存获取
        Product cachedProduct = (Product) redisTemplate.opsForValue().get(cacheKey);
        if (cachedProduct != null) {
            log.debug("Cache HIT for simple product: {}", productId);
            return cachedProduct;
        }

        log.debug("Cache MISS for simple product: {}", productId);

        // 2. 缓存未命中，查询数据库
        Product product = null;
        ProductPo productPo = this.findPoById(shopId, productId);
        product = CloneFactory.copy(new Product(), productPo);

        // 3. 写入缓存，设置随机过期时间防止雪崩
        redisTemplate.opsForValue().set(cacheKey, product,
                5 + (int)(Math.random() * 3), TimeUnit.MINUTES);

        return product;
    }

    /**
     * 创建Product对象 - 需要清理缓存
     *
     * @param product 传入的Product对象
     * @return 返回对象ReturnObj
     */
    public Product insert(Product product) throws BusinessException {
        // 注意：这里需要检查 requestVariables 是否为 null
        if (requestVariables == null) {
            log.warn("RequestVariables is null, using default creator");
            product.setCreatorId(1L);
            product.setCreatorName("system");
        } else {
            UserToken userToken = this.requestVariables.getUser();
            if (userToken != null) {
                product.setCreatorId(userToken.getId());
                product.setCreatorName(userToken.getName());
            }
        }

        ProductPo po = CloneFactory.copy(new ProductPo(), product);
        log.debug("insert: po = {}", po);
        ProductPo ret = this.productPoMapper.save(po);
        Product result = CloneFactory.copy(new Product(), ret);

        // 插入成功后，清理相关缓存
        clearProductCache(result.getId());

        return result;
    }

    /**
     * 修改商品信息 - 需要清理缓存
     *
     * @param product 传入的product对象
     * @return void
     */
    public void update(Product product) throws BusinessException {
        // 注意：这里需要检查 requestVariables 是否为 null
        if (requestVariables != null) {
            UserToken userToken = this.requestVariables.getUser();
            if (userToken != null) {
                product.setModifierId(userToken.getId());
                product.setModifierName(userToken.getName());
            }
        }

        log.debug("update: product = {}", product);

        Long shopId = (requestVariables != null && requestVariables.getUser() != null)
                ? requestVariables.getUser().getDepartId() : 0L;

        ProductPo oldPo = this.findPoById(shopId, product.getId());
        log.debug("update: oldPo = {}", oldPo);

        ProductPo newPo = CloneFactory.copyNotNull(oldPo, product);
        log.debug("update: newPo = {}", newPo);

        // 先保存数据库
        this.productPoMapper.save(newPo);

        // 清理相关缓存
        clearProductCache(product.getId());
    }

    /**
     * 删除商品 - 需要清理缓存
     *
     * @param id 商品id
     * @return
     */
    public void delete(Long id) throws BusinessException {
        Long shopId = (requestVariables != null && requestVariables.getUser() != null)
                ? requestVariables.getUser().getDepartId() : 0L;

        this.findPoById(shopId, id);
        this.productPoMapper.deleteById(id);

        // 清理相关缓存
        clearProductCache(id);
    }

    /**
     * 分开的Entity对象 - 带完整缓存
     * @param shopId 商铺id
     * @param productId 产品id
     * @return
     * @throws BusinessException
     */
    public Product findById(Long shopId, Long productId) throws BusinessException {
        String fullProductKey = PRODUCT_CACHE_KEY_PREFIX + "full:" + productId;

        // 1. 先尝试从缓存获取完整对象
        Product cachedProduct = (Product) redisTemplate.opsForValue().get(fullProductKey);
        if (cachedProduct != null) {
            log.debug("Cache HIT for full product: {}", productId);
            return cachedProduct;
        }

        log.debug("Cache MISS for full product: {}", productId);

        // 2. 缓存未命中，构建完整对象
        ProductPo productPo = this.findPoById(shopId, productId);
        Product product = this.getFullProduct(productPo);

        // 3. 写入缓存，设置过期时间
        redisTemplate.opsForValue().set(fullProductKey, product,
                10 + (int)(Math.random() * 5), TimeUnit.MINUTES);

        log.debug("findById: product = {}", product);
        return product;
    }

    /**
     *
     * @param shopId 商铺id 为PLATFORM则在全系统寻找，否则在商铺内寻找
     * @param name 名称
     * @return Product对象列表，带关联的Product返回
     */
    public List<Product> retrieveByName(Long shopId, String name) throws BusinessException {
        List<Product> productList = new ArrayList<>();
        Pageable pageable = PageRequest.of(0, 100);
        List<ProductPo> productPoList;
        if (PLATFORM.equals(shopId)) {
            productPoList = this.productPoMapper.findByName(name, pageable);
        } else {
            productPoList = this.productPoMapper.findByShopIdAndName(shopId, name, pageable);
        }
        for (ProductPo po : productPoList) {
            Product product = this.getFullProduct(po);
            productList.add(product);
        }
        log.debug("retrieveByName: productList = {}", productList);
        return productList;
    }

    /**
     * 获得关联的对象 - 带缓存
     * @param productPo product po对象
     * @return 关联的Product对象
     * @throws DataAccessException
     */
    private Product getFullProduct(@NotNull ProductPo productPo) throws DataAccessException {
        Product product = CloneFactory.copy(new Product(), productPo);
        log.debug("getFullProduct: product = {}", product);

        // 获取OnSale列表（带缓存）
        List<OnSale> latestOnSale = this.onSaleDao.getLatestOnSale(productPo.getId());
        product.setOnSaleList(latestOnSale);

        // 获取关联商品（带缓存）
        List<Product> otherProduct = this.retrieveOtherProduct(productPo);
        product.setOtherProduct(otherProduct);

        log.debug("getFullProduct: fullproduct = {}", product);
        return product;
    }

    /**
     * 获得相关的产品对象 - 带缓存
     * @param productPo product po对象
     * @return 相关产品对象列表
     * @throws DataAccessException
     */
    private List<Product> retrieveOtherProduct(@NotNull ProductPo productPo) throws DataAccessException {
        String relationKey = PRODUCT_RELATION_KEY_PREFIX + productPo.getId();

        // 1. 尝试从关系缓存获取
        Set<Object> cachedIds = redisTemplate.opsForSet().members(relationKey);
        if (cachedIds != null && !cachedIds.isEmpty()) {
            log.debug("Cache HIT for product relation: {}", productPo.getId());

            // 批量获取商品对象缓存
            List<String> productKeys = cachedIds.stream()
                    .map(id -> PRODUCT_CACHE_KEY_PREFIX + id)
                    .collect(Collectors.toList());

            List<Object> cachedProducts = redisTemplate.opsForValue().multiGet(productKeys);
            return cachedProducts.stream()
                    .filter(Objects::nonNull)
                    .map(obj -> (Product) obj)
                    .collect(Collectors.toList());
        }

        log.debug("Cache MISS for product relation: {}", productPo.getId());

        // 2. 缓存未命中，查询数据库
        List<ProductPo> productPoList;
        List<GoodsPo> goodsPos = this.goodsPoMapper.findByProductId(productPo.getId());
        List<Long> productIds = goodsPos.stream()
                .map(GoodsPo::getRelateProductId)
                .collect(Collectors.toList());

        if (productIds.isEmpty()) {
            // 防止缓存穿透：缓存空结果
            redisTemplate.opsForSet().add(relationKey, "empty");
            redisTemplate.expire(relationKey, 2, TimeUnit.MINUTES);
            return Collections.emptyList();
        }

        productPoList = this.productPoMapper.findByIdIn(productIds);
        List<Product> result = productPoList.stream()
                .map(po -> CloneFactory.copy(new Product(), po))
                .collect(Collectors.toList());

        // 3. 构建关系缓存和对象缓存
        redisTemplate.opsForSet().add(relationKey, productIds.toArray());
        redisTemplate.expire(relationKey, 30, TimeUnit.MINUTES); // 商品关系相对稳定

        // 缓存每个商品对象
        for (Product product : result) {
            String productKey = PRODUCT_CACHE_KEY_PREFIX + product.getId();
            redisTemplate.opsForValue().set(productKey, product,
                    10 + (int)(Math.random() * 5), TimeUnit.MINUTES);
        }

        return result;
    }

    /**
     * 找到po对象，判断对象是否存在以及是否属于本商铺
     * @param shopId 商铺id
     * @param productId 商品id
     * @return RESOURCE_ID_OUTSCOPE, RESOURCE_ID_NOTEXIST
     */
    private ProductPo findPoById(Long shopId, Long productId) {
        ProductPo productPo = this.productPoMapper.findById(productId).orElseThrow(() ->
                new BusinessException(ReturnNo.RESOURCE_ID_NOTEXIST,
                        JacksonUtil.toJson(new String[]{"${product}", productId.toString()})));

        log.debug("findPoById: shopId = {}, productPo = {}", shopId, productPo);

        if (!Objects.equals(shopId, productPo.getShopId()) && !PLATFORM.equals(shopId)) {
            String[] objects = new String[]{"${product}", productId.toString(), shopId.toString()};
            throw new BusinessException(ReturnNo.RESOURCE_ID_OUTSCOPE, JacksonUtil.toJson(objects));
        }
        return productPo;
    }

    /**
     * 清理商品相关的所有缓存
     * @param productId 商品ID
     */
    private void clearProductCache(Long productId) {
        // 清理对象缓存
        String productKey = PRODUCT_CACHE_KEY_PREFIX + productId;
        String fullProductKey = PRODUCT_CACHE_KEY_PREFIX + "full:" + productId;

        // 清理关系缓存
        String productRelationKey = PRODUCT_RELATION_KEY_PREFIX + productId;
        String productOnsaleRelationKey = PRODUCT_ONSALE_RELATION_KEY_PREFIX + productId;

        // 批量删除
        redisTemplate.delete(Arrays.asList(
                productKey,
                fullProductKey,
                productRelationKey,
                productOnsaleRelationKey
        ));

        log.debug("Cleared all cache for product: {}", productId);
    }
}