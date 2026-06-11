"""
Equivalente Python de com.classify20.model.Agenda (entidad JPA).

IMPORTANTE: managed = False
La tabla "agendas" ya existe y la administra Hibernate (ddl-auto=update)
desde la app Spring Boot. Django solo LEE y ESCRIBE en ella; nunca la
crea, altera ni borra. Así no se daña la conexión ni el esquema actual.

Los nombres de columna siguen la convención de Spring Boot/Hibernate:
camelCase → snake_case (horaInicio → hora_inicio, etc.).
"""
from django.db import models


class Agenda(models.Model):
    id = models.BigAutoField(primary_key=True)

    # Datos del salón
    grado = models.IntegerField(null=True, blank=True)
    grupo = models.CharField(max_length=255, null=True, blank=True)

    # Detalles de la clase
    profesor = models.CharField(max_length=255, null=True, blank=True)
    materia = models.CharField(max_length=255, null=True, blank=True)
    fecha = models.DateField(null=True, blank=True)
    hora_inicio = models.TimeField(db_column="hora_inicio", null=True, blank=True)
    duracion = models.IntegerField(null=True, blank=True)
    modalidad = models.CharField(max_length=255, null=True, blank=True)

    # Objetivos y temas
    tema_principal = models.CharField(
        db_column="tema_principal", max_length=255, null=True, blank=True
    )
    objetivos = models.TextField(null=True, blank=True)
    dificultades = models.TextField(null=True, blank=True)

    # Recursos
    materiales_basicos = models.CharField(
        db_column="materiales_basicos", max_length=255, null=True, blank=True
    )  # "si", "no", "parcialmente"
    recursos_necesarios = models.TextField(
        db_column="recursos_necesarios", null=True, blank=True
    )

    class Meta:
        db_table = "agendas"
        managed = False  # la tabla la administra Hibernate, no Django

    def __str__(self):
        return f"Agenda #{self.id} - {self.materia} ({self.fecha})"
