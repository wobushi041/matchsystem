package com.wobushi041.matchsystem.model.enums;

/**
 * 队伍状态枚举类。
 * 定义了队伍的状态枚举值及其对应的数值和描述信息。
 */
public enum TeamStatusEnum {

    PUBLIC(0, "公开"),
    PRIVATE(1, "私有"),
    SECRET(2, "加密");

    /**
     * 枚举值
     */
    private int value;

    /**
     * 描述文本
     */
    private String text;

    /**
     * 根据数值获取对应的枚举值
     * @param value 数值
     * @return 对应的枚举值，若无对应值则返回null
     */
    public static TeamStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        TeamStatusEnum[] values = TeamStatusEnum.values();
        for (TeamStatusEnum teamStatusEnum : values) {
            if (teamStatusEnum.getValue() == value) {
                return teamStatusEnum;
            }
        }
        return null;
    }

    TeamStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

}