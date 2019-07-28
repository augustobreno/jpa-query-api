package br.com.vcg.query.repository;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.ComparablePath;

@SuppressWarnings({ "serial", "rawtypes" })
public class ComparablePathCustom<T extends Comparable> extends ComparablePath<T> {
    protected ComparablePathCustom(Class<? extends T> type, Path<?> parent, String property) {
        super(type, parent, property);
    }
}
