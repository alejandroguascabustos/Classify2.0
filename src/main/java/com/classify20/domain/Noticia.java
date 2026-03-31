package com.classify20.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

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

    // ← DATETIME en lugar de DATE
    @Column(name = "fecha_noticia", nullable = false)
    private LocalDateTime fechaNoticia;

    @Column(name = "contenido_noticia", nullable = false, columnDefinition = "TEXT")
    private String contenidoNoticia;

    @Column(name = "tipo_noticia")
    private String tipoNoticia;

    @Column(name = "imagen_noticia", length = 500)
    private String imagenNoticia;
}