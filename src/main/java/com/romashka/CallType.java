package com.romashka;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * Перечисление, представляющее типы звонков.
 * Содержит два типа: исходящий и входящий звонок.
 */
@Getter
@AllArgsConstructor
public enum CallType {

    /**
     * Тип звонка — исходящий.
     * Обозначается кодом "01".
     */
    OUTGOING("01"),

    /**
     * Тип звонка — входящий.
     * Обозначается кодом "02".
     */
    INCOMING("02");

    /**
     * Код типа звонка.
     */
    private final String code;

    public String getCode() {
        return code;
    }

    /**
     * Получает {@link CallType} по переданному коду.
     *
     * @param code Код типа звонка.
     * @return {@link CallType}, соответствующий переданному коду.
     * @throws IllegalArgumentException если код не соответствует ни одному типу звонка.
     */
    public static CallType fromCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown code: " + code));
    }
}
