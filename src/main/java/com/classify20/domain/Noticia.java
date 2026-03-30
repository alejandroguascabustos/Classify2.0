package com.classify20.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "noticias")
public class Noticia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_noticia")
    private Long idNoticia;

    @Column(name = "titulo_noticia", nullable = false)
    private String tituloNoticia;

    @Column(name = "autor_noticia", nullable = false)
    private String autorNoticia;

    @Column(name = "fecha_noticia", nullable = false)
    private LocalDate fechaNoticia;

    @Column(name = "contenido_noticia", nullable = false, columnDefinition = "TEXT")
    private String contenidoNoticia;

    @Column(name = "tipo_noticia")
    private String tipoNoticia;
}
