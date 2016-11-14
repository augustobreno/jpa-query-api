package br.com.vcg.query.repository;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.StringPath;

@SuppressWarnings("serial")
public class StringPathCustom extends StringPath {
    protected StringPathCustom(Path<?> parent, String property) {
        super(parent, property);
    }
}
