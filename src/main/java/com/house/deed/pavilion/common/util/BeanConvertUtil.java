package com.house.deed.pavilion.common.util;

import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 通用实体转换工具（所有DTO/实体转换统一复用）
 */
public class BeanConvertUtil {

    /**
     * 单个对象转换：DTO→实体 或 实体→DTO
     * @param source 源对象（DTO/实体）
     * @param targetClass 目标类（实体/DTO.class）
     * @param <S> 源类型
     * @param <T> 目标类型
     * @return 转换后的目标对象
     */
    public static <S, T> T convert(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        T target;
        try {
            target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("实体转换失败：" + e.getMessage(), e);
        }
    }

    /**
     * 单个对象转换（支持自定义字段处理）
     * @param source 源对象
     * @param targetClass 目标类
     * @param function 自定义字段处理逻辑（如特殊格式转换）
     * @param <S> 源类型
     * @param <T> 目标类型
     * @return 转换后的目标对象
     */
    public static <S, T> T convert(S source, Class<T> targetClass, Function<T, T> function) {
        T target = convert(source, targetClass);
        return function.apply(target);
    }

    /**
     * 集合转换：DTO列表→实体列表 或 实体列表→DTO列表
     * @param sourceList 源列表
     * @param targetClass 目标类
     * @param <S> 源类型
     * @param <T> 目标类型
     * @return 转换后的目标列表
     */
    public static <S, T> List<T> convertList(List<S> sourceList, Class<T> targetClass) {
        if (CollectionUtils.isEmpty(sourceList)) {
            return new ArrayList<>();
        }
        List<T> targetList = new ArrayList<>(sourceList.size());
        for (S source : sourceList) {
            targetList.add(convert(source, targetClass));
        }
        return targetList;
    }

    /**
     * 集合转换（支持自定义字段处理）
     * @param sourceList 源列表
     * @param targetClass 目标类
     * @param function 自定义字段处理逻辑
     * @param <S> 源类型
     * @param <T> 目标类型
     * @return 转换后的目标列表
     */
    public static <S, T> List<T> convertList(List<S> sourceList, Class<T> targetClass, Function<T, T> function) {
        if (CollectionUtils.isEmpty(sourceList)) {
            return new ArrayList<>();
        }
        List<T> targetList = new ArrayList<>(sourceList.size());
        for (S source : sourceList) {
            T target = convert(source, targetClass);
            targetList.add(function.apply(target));
        }
        return targetList;
    }
}