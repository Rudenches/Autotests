package org.example;



import org.example.CallType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Класс для представления данных звонка (CDR).
 * Реализует интерфейс Serializable для использования с RabbitMQ.
 */

@Table(name = "cdr_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CdrData implements Serializable {

    /**
     * Уникальный идентификатор записи.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long callId;

    /**
     * Тип звонка:
     * <ul>
     *   <li>"01" — исходящий звонок.</li>
     *   <li>"02" — входящий звонок.</li>
     * </ul>
     */
    private CallType callType;

    /**
     * Номер абонента, инициирующего звонок.
     */
    private String callerNumber;

    /**
     * Номер абонента, принимающего звонок.
     */
    private String contactNumber;

    /**
     * Дата и время начала звонка в формате ISO 8601.
     */
    private LocalDateTime startTime;

    /**
     * Дата и время окончания звонка в формате ISO 8601.
     */
    private LocalDateTime endTime;

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(startTime.toString());
        out.writeObject(endTime.toString());
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        startTime = LocalDateTime.parse((String) in.readObject());
        endTime = LocalDateTime.parse((String) in.readObject());
    }
}