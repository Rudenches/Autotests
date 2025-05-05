package org.example;

import java.io.IOException;

public class CdrGeneratorExample {
    public static void main(String[] args) throws IOException {
        // Генерация правильного файла
        CdrFileGenerator.generateValidCdrFile("valid_cdr.csv", 5);

        // Генерация файлов с различными ошибками
        InvalidCdrFileGenerator.generateInvalidCdrFile(
                "invalid_time.csv",
                InvalidCdrFileGenerator.InvalidType.INVALID_TIME
        );

        InvalidCdrFileGenerator.generateInvalidCdrFile(
                "invalid_number_format.csv",
                InvalidCdrFileGenerator.InvalidType.INVALID_NUMBER_FORMAT
        );

        InvalidCdrFileGenerator.generateInvalidCdrFile(
                "invalid_missing_fields.csv",
                InvalidCdrFileGenerator.InvalidType.MISSING_FIELDS
        );

        // и т.д.
    }
}