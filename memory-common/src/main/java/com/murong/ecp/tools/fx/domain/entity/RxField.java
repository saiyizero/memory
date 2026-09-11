package com.murong.ecp.tools.fx.domain.entity;

import com.murong.ecp.tools.fx.enums.DataStatusEnum;
import lombok.Data;

import java.util.List;

@Data
public class RxField {
    private String nameCamel;
    private String nameSnake;
    private String type;
    private String commentCn;
    private String commentEn;
    private String dbTyp;
    private Integer length;
    private boolean notNull;
    private String defaultValue;
    private String typeRef;
    private String enumRef;
    private String enumNme;
    private DataStatusEnum bizUpdSts;
    private List<RxField> children;
}
