package com.wms.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Generic CRUD endpoints for master data.
 * GET  /page?current=&size=&keyword=&{field}=   paged query
 * GET  /list                                     all rows (for drop-downs)
 * GET  /{id}
 * POST /                                         create
 * PUT  /{id}                                     update
 * DELETE /{id}
 */
public abstract class BaseCrudController<T extends BaseEntity, M extends BaseMapper<T>> {

    @Autowired
    protected M mapper;

    private final Class<T> entityClass;

    protected BaseCrudController(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    /** columns searched with LIKE when keyword is given */
    protected abstract String[] keywordColumns();

    @GetMapping("/page")
    public R<Page<T>> page(@RequestParam(defaultValue = "1") long current,
                           @RequestParam(defaultValue = "20") long size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam Map<String, String> params) {
        QueryWrapper<T> qw = new QueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            qw.and(w -> {
                for (String col : keywordColumns()) {
                    w.or().like(col, keyword);
                }
            });
        }
        applyFilters(qw, params);
        qw.orderByDesc("id");
        return R.ok(mapper.selectPage(new Page<>(current, size), qw));
    }

    @GetMapping("/list")
    public R<List<T>> list(@RequestParam Map<String, String> params) {
        QueryWrapper<T> qw = new QueryWrapper<>();
        applyFilters(qw, params);
        qw.orderByAsc("id");
        return R.ok(mapper.selectList(qw));
    }

    @GetMapping("/{id}")
    public R<T> get(@PathVariable Long id) {
        return R.ok(mapper.selectById(id));
    }

    @PostMapping
    public R<T> create(@RequestBody T entity) {
        entity.setId(null);
        beforeSave(entity);
        mapper.insert(entity);
        return R.ok(entity);
    }

    @PutMapping("/{id}")
    public R<T> update(@PathVariable Long id, @RequestBody T entity) {
        entity.setId(id);
        beforeSave(entity);
        mapper.updateById(entity);
        return R.ok(entity);
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        mapper.deleteById(id);
        return R.ok();
    }

    protected void beforeSave(T entity) {
    }

    private static final List<String> RESERVED = Arrays.asList("current", "size", "keyword");

    /** exact-match filters on any real column, e.g. ?warehouseCode=WH01 */
    private void applyFilters(QueryWrapper<T> qw, Map<String, String> params) {
        TableInfo info = TableInfoHelper.getTableInfo(entityClass);
        params.forEach((k, v) -> {
            if (RESERVED.contains(k) || StringUtils.isBlank(v)) {
                return;
            }
            info.getFieldList().stream()
                    .filter(f -> f.getProperty().equals(k))
                    .findFirst()
                    .ifPresent(f -> qw.eq(f.getColumn(), v));
        });
    }
}
