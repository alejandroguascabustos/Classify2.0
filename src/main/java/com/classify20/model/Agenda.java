package com.classify20.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "agendas")
@Data               // Lombok: genera getters, setters, toString, equals
@NoArgsConstructor  // Lombok: constructor vacío (requerido por JPA)
@AllArgsConstructor // Lombok: constructor con todos los campos
@Builder            // Lombok: patrón builder (opcional pero útil)
public class Agenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Datos del salón ---
    private Integer grado;
    private String grupo;

    //  Detalles de la clase ---
    private String profesor;
    private String materia;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private Integer duracion;
    private String modalidad;

    //  Objetivos y temas ---
    private String temaPrincipal;

    @Column(columnDefinition = "TEXT")
    private String objetivos;

    @Column(columnDefinition = "TEXT")
    private String dificultades;

    //  Recursos ---
    private String materialesBasicos;  // "si", "no", "parcialmente"

    @Column(columnDefinition = "TEXT")
    private String recursosNecesarios;

}
