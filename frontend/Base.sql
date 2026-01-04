-- =====================================================================
-- Base.sql
-- Base de datos para aplicacion web graficadora de diagramas de casos
-- de uso UML con soporte multimedia (MP3, MP4, JPG).
-- =====================================================================

DROP DATABASE IF EXISTS `aplicacion`;
CREATE DATABASE `aplicacion` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_spanish_ci;
USE `aplicacion`;

SET FOREIGN_KEY_CHECKS=0;
SET SQL_MODE='NO_AUTO_VALUE_ON_ZERO';

-- ---------------------------------------------------------------------
-- Roles
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles` (
  `id_rol`      TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `nombre_rol`  VARCHAR(45) NOT NULL,
  `descripcion` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id_rol`),
  UNIQUE KEY `uk_roles_nombre_rol` (`nombre_rol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- ---------------------------------------------------------------------
-- Usuarios
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `usuarios`;
CREATE TABLE `usuarios` (
  `id_usuario`          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `nombre_usuario`      VARCHAR(45) NOT NULL,
  `correo`              VARCHAR(120) DEFAULT NULL,
  `contrasena`          VARCHAR(255) NOT NULL,
  `id_rol`              TINYINT UNSIGNED NOT NULL,
  `fecha_creacion`      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_actualizacion` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `uk_usuarios_nombre_usuario` (`nombre_usuario`),
  UNIQUE KEY `uk_usuarios_correo` (`correo`),
  KEY `idx_usuarios_rol` (`id_rol`),
  CONSTRAINT `fk_usuarios_rol`
    FOREIGN KEY (`id_rol`) REFERENCES `roles` (`id_rol`)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- ---------------------------------------------------------------------
-- Diagramas UML
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `diagramas_uml`;
CREATE TABLE `diagramas_uml` (
  `id_diagrama`   INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_usuario`    INT UNSIGNED NOT NULL,
  `nombre`        VARCHAR(120) NOT NULL,
  `descripcion`   TEXT DEFAULT NULL,
  `estado`        ENUM('BORRADOR','ACTIVO','ARCHIVADO') NOT NULL DEFAULT 'ACTIVO',
  `ancho_lienzo`  INT NOT NULL DEFAULT 1280,
  `alto_lienzo`   INT NOT NULL DEFAULT 720,
  `configuracion_json` JSON DEFAULT NULL,
  `fecha_creacion` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_actualizacion` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_diagrama`),
  KEY `idx_diagramas_usuario` (`id_usuario`),
  KEY `idx_diagramas_estado` (`estado`),
  CONSTRAINT `fk_diagramas_usuario`
    FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- ---------------------------------------------------------------------
-- Elementos Graficos
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `elementos_diagrama`;
CREATE TABLE `elementos_diagrama` (
  `id_elemento`         INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_diagrama`         INT UNSIGNED NOT NULL,
  `id_elemento_padre`   INT UNSIGNED DEFAULT NULL,
  `tipo_elemento`       ENUM(
    'ACTOR',
    'CASO_DE_USO',
    'LIMITE_SISTEMA',
    'PAQUETE',
    'NOTA',
    'TEXTO',
    'IMAGEN'
  ) NOT NULL,
  `etiqueta`            VARCHAR(255) DEFAULT NULL,
  `pos_x`               INT NOT NULL DEFAULT 0,
  `pos_y`               INT NOT NULL DEFAULT 0,
  `ancho`               INT NOT NULL DEFAULT 120,
  `alto`                INT NOT NULL DEFAULT 60,
  `rotacion_grados`     DECIMAL(6,2) NOT NULL DEFAULT 0.00,
  `orden_z`             INT NOT NULL DEFAULT 0,
  `estilo_json`         JSON DEFAULT NULL,
  `metadatos_json`      JSON DEFAULT NULL,
  `fecha_creacion`      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_actualizacion` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_elemento`),
  KEY `idx_elementos_diagrama` (`id_diagrama`),
  KEY `idx_elementos_padre` (`id_elemento_padre`),
  CONSTRAINT `fk_elementos_diagrama`
    FOREIGN KEY (`id_diagrama`) REFERENCES `diagramas_uml` (`id_diagrama`)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT `fk_elementos_padre`
    FOREIGN KEY (`id_elemento_padre`) REFERENCES `elementos_diagrama` (`id_elemento`)
    ON UPDATE CASCADE
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- ---------------------------------------------------------------------
-- Relaciones Entre Elementos
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `conexiones_diagrama`;
CREATE TABLE `conexiones_diagrama` (
  `id_conexion`         INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_diagrama`         INT UNSIGNED NOT NULL,
  `id_elemento_origen`  INT UNSIGNED NOT NULL,
  `id_elemento_destino` INT UNSIGNED NOT NULL,
  `tipo_conexion`       ENUM(
    'ASOCIACION',
    'INCLUSION',
    'EXTENSION',
    'GENERALIZACION',
    'DEPENDENCIA',
    'ENLACE_NOTA'
  ) NOT NULL,
  `etiqueta`            VARCHAR(255) DEFAULT NULL,
  `puntos_json`         JSON DEFAULT NULL,
  `estilo_json`         JSON DEFAULT NULL,
  `fecha_creacion`      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_actualizacion` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_conexion`),
  KEY `idx_conexiones_diagrama` (`id_diagrama`),
  KEY `idx_conexiones_origen` (`id_elemento_origen`),
  KEY `idx_conexiones_destino` (`id_elemento_destino`),
  CONSTRAINT `fk_conexiones_diagrama`
    FOREIGN KEY (`id_diagrama`) REFERENCES `diagramas_uml` (`id_diagrama`)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT `fk_conexiones_origen`
    FOREIGN KEY (`id_elemento_origen`) REFERENCES `elementos_diagrama` (`id_elemento`)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT `fk_conexiones_destino`
    FOREIGN KEY (`id_elemento_destino`) REFERENCES `elementos_diagrama` (`id_elemento`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- ---------------------------------------------------------------------
-- Multimedia (MP3, MP4, JPG)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `archivos_multimedia`;
CREATE TABLE `archivos_multimedia` (
  `id_archivo`          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_usuario`          INT UNSIGNED NOT NULL,
  `tipo_media`          ENUM('IMAGEN','AUDIO','VIDEO') NOT NULL,
  `titulo`              VARCHAR(120) DEFAULT NULL,
  `descripcion`         TEXT DEFAULT NULL,
  `tamano_bytes`        BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `duracion_segundos`   DECIMAL(10,2) DEFAULT NULL,
  `ancho`               INT DEFAULT NULL,
  `alto`                INT DEFAULT NULL,
  `ruta_archivo`        VARCHAR(500) NOT NULL,
  `fecha_creacion`      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_actualizacion` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_archivo`),
  KEY `idx_multimedia_usuario` (`id_usuario`),
  KEY `idx_multimedia_tipo` (`tipo_media`),
  CONSTRAINT `fk_multimedia_usuario`
    FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT `chk_multimedia_ruta_extension`
    CHECK (
      (`tipo_media` = 'AUDIO' AND `ruta_archivo` LIKE '%.mp3') OR
      (`tipo_media` = 'VIDEO' AND `ruta_archivo` LIKE '%.mp4') OR
      (`tipo_media` = 'IMAGEN' AND (`ruta_archivo` LIKE '%.jpg' OR `ruta_archivo` LIKE '%.jpeg'))
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- ---------------------------------------------------------------------
-- Asociacion Multimedia <-> Diagramas
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `diagrama_multimedia`;
CREATE TABLE `diagrama_multimedia` (
  `id_diagrama`         INT UNSIGNED NOT NULL,
  `id_archivo`          INT UNSIGNED NOT NULL,
  `descripcion`         VARCHAR(255) DEFAULT NULL,
  `orden`               INT NOT NULL DEFAULT 0,
  `fecha_creacion`      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_diagrama`, `id_archivo`),
  KEY `idx_diagrama_multimedia_archivo` (`id_archivo`),
  CONSTRAINT `fk_diagrama_multimedia_diagrama`
    FOREIGN KEY (`id_diagrama`) REFERENCES `diagramas_uml` (`id_diagrama`)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT `fk_diagrama_multimedia_archivo`
    FOREIGN KEY (`id_archivo`) REFERENCES `archivos_multimedia` (`id_archivo`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- ---------------------------------------------------------------------
-- Asociacion Multimedia <-> Elementos
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `elemento_multimedia`;
CREATE TABLE `elemento_multimedia` (
  `id_elemento`         INT UNSIGNED NOT NULL,
  `id_archivo`          INT UNSIGNED NOT NULL,
  `tipo_uso`            ENUM('ICONO','FONDO','ADJUNTO') NOT NULL DEFAULT 'ADJUNTO',
  `fecha_creacion`      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_elemento`, `id_archivo`),
  KEY `idx_elemento_multimedia_archivo` (`id_archivo`),
  CONSTRAINT `fk_elemento_multimedia_elemento`
    FOREIGN KEY (`id_elemento`) REFERENCES `elementos_diagrama` (`id_elemento`)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT `fk_elemento_multimedia_archivo`
    FOREIGN KEY (`id_archivo`) REFERENCES `archivos_multimedia` (`id_archivo`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- ---------------------------------------------------------------------
-- Datos Iniciales
-- ---------------------------------------------------------------------
INSERT INTO `roles` (`id_rol`, `nombre_rol`, `descripcion`) VALUES
  (1, 'ADMIN'   , 'USUARIO CON ACCESO TOTAL AL SISTEMA'),
  (2, 'USUARIO' , 'USUARIO CON ACCESO LIMITADO AL SISTEMA');

INSERT INTO `usuarios` (`id_usuario`, `nombre_usuario`, `contrasena`, `id_rol`) VALUES
  (1, 'ADMINISTRADOR', '1234', 1);

SET FOREIGN_KEY_CHECKS=1;