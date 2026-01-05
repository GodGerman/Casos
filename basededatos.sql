-- =====================================================================
-- basededatos.sql
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
-- Tabla catalogo de roles del sistema.
-- Finalidad: Definir perfiles de acceso que habilitan o restringen acciones
-- dentro del sistema (por ejemplo, un ADMIN puede administrar usuarios).
-- Conexiones: `usuarios.id_rol` referencia `roles.id_rol`, por lo que
-- un rol puede estar asignado a muchos usuarios.
-- Integridad: Nombre de rol unico; no se permite borrar un rol en uso
-- porque los usuarios dependen de el (ON DELETE RESTRICT).
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
-- Tabla de cuentas que acceden a la aplicacion.
-- Finalidad: Almacenar credenciales, rol y fechas para control de acceso
-- y auditoria basica de actividad.
-- Conexiones:
--   - FK `id_rol` -> `roles.id_rol` (permite controlar permisos).
--   - FK desde `diagramas_uml.id_usuario` (cada diagrama tiene propietario).
--   - FK desde `archivos_multimedia.id_usuario` (cada archivo tiene propietario).
-- Integridad:
--   - `nombre_usuario` y `correo` son unicos para evitar duplicados.
--   - El rol no puede borrarse si esta asignado (RESTRICT).
--   - Al borrar un usuario se eliminan sus diagramas/archivos (CASCADE).
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
-- Cabecera de diagramas UML (casos de uso).
-- Finalidad: Representar un proyecto/diagrama y su configuracion general
-- (nombre, estado, dimensiones y preferencias del editor).
-- Conexiones:
--   - FK `id_usuario` -> `usuarios.id_usuario` (propietario del diagrama).
--   - Referenciada por `elementos_diagrama`, `conexiones_diagrama`
--     y `diagrama_multimedia` para cargar todo el contenido asociado.
-- Datos:
--   - `estado` controla el ciclo de vida (BORRADOR/ACTIVO/ARCHIVADO).
--   - `ancho_lienzo`/`alto_lienzo` define el tamano del canvas.
--   - `configuracion_json` guarda preferencias del editor (opcional).
-- Integridad: Al borrar un diagrama se eliminan sus elementos, conexiones
-- y asociaciones multimedia (ON DELETE CASCADE).
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
-- Elementos que se dibujan en el lienzo de un diagrama.
-- Finalidad: Modelar actores, casos de uso, limites del sistema,
-- paquetes, notas, texto e imagenes que aparecen en el canvas.
-- Conexiones:
--   - FK `id_diagrama` -> `diagramas_uml.id_diagrama`.
--   - FK `id_elemento_padre` -> `elementos_diagrama.id_elemento`
--     (permite jerarquia: elementos dentro de limites o paquetes).
--   - Referenciada por `conexiones_diagrama` como origen/destino.
--   - Referenciada por `elemento_multimedia` para adjuntar recursos.
-- Datos:
--   - `pos_x`, `pos_y`, `ancho`, `alto`, `rotacion_grados`, `orden_z`
--     controlan posicion, tamano y orden visual en el canvas.
--   - `estilo_json`/`metadatos_json` guardan estilos o datos extendidos.
-- Integridad:
--   - Al borrar el diagrama se eliminan los elementos (CASCADE).
--   - Si se borra un padre, el hijo queda sin padre (SET NULL).
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
-- Conexiones UML entre elementos dentro del mismo diagrama.
-- Finalidad: Representar asociaciones, inclusiones, extensiones,
-- generalizaciones, dependencias y enlaces a notas.
-- Conexiones:
--   - FK `id_diagrama` -> `diagramas_uml.id_diagrama`.
--   - FK `id_elemento_origen` -> `elementos_diagrama.id_elemento`.
--   - FK `id_elemento_destino` -> `elementos_diagrama.id_elemento`.
-- Datos:
--   - `tipo_conexion` define el estilo de linea y flecha en el editor.
--   - `etiqueta` permite texto (por ejemplo <<include>>).
--   - `puntos_json`/`estilo_json` permiten rutas y estilos personalizados.
-- Integridad: Al borrar diagramas o elementos se eliminan conexiones
-- para evitar referencias colgantes (CASCADE).
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
-- Catalogo de archivos multimedia subidos por los usuarios.
-- Finalidad: Almacenar metadatos y la ruta del archivo en disco/servidor
-- para poder listar, previsualizar y reutilizar recursos.
-- Conexiones:
--   - FK `id_usuario` -> `usuarios.id_usuario` (propietario del archivo).
--   - Referenciada por `diagrama_multimedia` (adjuntos por diagrama).
--   - Referenciada por `elemento_multimedia` (adjuntos por elemento).
-- Datos:
--   - `ruta_archivo` apunta al archivo fisico que sirve el backend.
--   - `tipo_media` y `ruta_archivo` deben coincidir via CHECK.
--   - `tamano_bytes`, `duracion_segundos`, `ancho`, `alto` para metadatos.
-- Integridad: Al borrar el usuario se eliminan sus archivos (CASCADE).
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
-- Relacion muchos-a-muchos entre diagramas y archivos multimedia.
-- Finalidad: Permitir adjuntar recursos (audio, video, imagen) a un
-- diagrama y mostrarlos en el modulo de multimedia del proyecto.
-- Conexiones:
--   - FK `id_diagrama` -> `diagramas_uml.id_diagrama`.
--   - FK `id_archivo` -> `archivos_multimedia.id_archivo`.
-- Datos:
--   - `descripcion` permite anotar el uso del archivo en el diagrama.
--   - `orden` controla el orden de despliegue en la interfaz.
-- Integridad: PK compuesta evita duplicados; ON DELETE CASCADE al
-- borrar el diagrama o el archivo.
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
-- Relacion muchos-a-muchos entre elementos y archivos multimedia.
-- Finalidad: Adjuntar iconos, fondos o archivos a un elemento UML
-- (por ejemplo, una imagen como icono o un adjunto para referencia).
-- Conexiones:
--   - FK `id_elemento` -> `elementos_diagrama.id_elemento`.
--   - FK `id_archivo` -> `archivos_multimedia.id_archivo`.
-- Datos:
--   - `tipo_uso` define como se interpreta en UI: ICONO, FONDO o ADJUNTO.
-- Integridad: PK compuesta evita duplicados; ON DELETE CASCADE al
-- borrar el elemento o el archivo.
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
-- Carga minima para iniciar sesion y probar el flujo base del sistema.
-- Incluye rol ADMIN, usuario ADMIN y ejemplos funcionales.
INSERT INTO `roles` (`id_rol`, `nombre_rol`, `descripcion`) VALUES
  (1, 'ADMIN', 'USUARIO CON ACCESO TOTAL AL SISTEMA');

INSERT INTO `usuarios` (`id_usuario`, `nombre_usuario`, `contrasena`, `id_rol`) VALUES
  (1, 'ADMIN', '1234', 1);

-- ---------------------------------------------------------------------
-- Diagramas de Casos de Uso (Ejemplos Iniciales)
-- ---------------------------------------------------------------------
INSERT INTO `diagramas_uml` (
  `id_diagrama`, `id_usuario`, `nombre`, `descripcion`, `estado`,
  `ancho_lienzo`, `alto_lienzo`, `configuracion_json`
) VALUES
  (1, 1, 'Gestion de Diagramas'     , 'Casos de uso para crear, editar, exportar y eliminar diagramas.'         , 'ACTIVO', 1280, 720, NULL),
  (2, 1, 'Autenticacion y Usuarios' , 'Casos de uso para inicio/cierre de sesion y administracion de usuarios.' , 'ACTIVO', 1280, 720, NULL),
  (3, 1, 'Gestion de Multimedia'    , 'Casos de uso para subir, asociar y administrar archivos multimedia.'     , 'ACTIVO', 1280, 720, NULL);

INSERT INTO `elementos_diagrama` (
  `id_elemento`, `id_diagrama`, `id_elemento_padre`, `tipo_elemento`, `etiqueta`,
  `pos_x`, `pos_y`, `ancho`, `alto`, `rotacion_grados`, `orden_z`, `estilo_json`, `metadatos_json`
) VALUES
  -- Diagrama 1: Gestion de Diagramas
  (1 , 1, NULL, 'ACTOR'           , 'Usuario'                             , 40  , 240 , 80  , 140 , 0.00, 10, NULL, NULL),
  (2 , 1, NULL, 'LIMITE_SISTEMA'  , 'Sistema de Diagramas'                , 180 , 80  , 900 , 520 , 0.00, 1 , NULL, NULL),
  (3 , 1, NULL, 'CASO_DE_USO'     , 'Crear diagrama'                      , 260 , 140 , 220 , 70  , 0.00, 10, NULL, NULL),
  (4 , 1, NULL, 'CASO_DE_USO'     , 'Editar diagrama'                     , 260 , 260 , 220 , 70  , 0.00, 10, NULL, NULL),
  (5 , 1, NULL, 'CASO_DE_USO'     , 'Eliminar diagrama'                   , 260 , 380 , 220 , 70  , 0.00, 10, NULL, NULL),
  (6 , 1, NULL, 'CASO_DE_USO'     , 'Exportar diagrama'                   , 560 , 140 , 220 , 70  , 0.00, 10, NULL, NULL),
  (7 , 1, NULL, 'CASO_DE_USO'     , 'Guardar cambios'                     , 560 , 260 , 220 , 70  , 0.00, 10, NULL, NULL),
  -- Diagrama 2: Autenticacion y Usuarios
  (8 , 2, NULL, 'ACTOR'           , 'Usuario'                             , 40  , 240 , 80  , 140 , 0.00, 10, NULL, NULL),
  (9 , 2, NULL, 'ACTOR'           , 'Administrador'                       , 1100, 240 , 80  , 140 , 0.00, 10, NULL, NULL),
  (10, 2, NULL, 'LIMITE_SISTEMA'  , 'Sistema de Autenticacion'            , 180 , 80  , 900 , 520 , 0.00, 1 , NULL, NULL),
  (11, 2, NULL, 'CASO_DE_USO'     , 'Iniciar sesion'                      , 420 , 140 , 220 , 70  , 0.00, 10, NULL, NULL),
  (12, 2, NULL, 'CASO_DE_USO'     , 'Cerrar sesion'                       , 420 , 260 , 220 , 70  , 0.00, 10, NULL, NULL),
  (13, 2, NULL, 'CASO_DE_USO'     , 'Gestionar usuarios'                  , 640 , 200 , 240 , 70  , 0.00, 10, NULL, NULL),
  (14, 2, NULL, 'CASO_DE_USO'     , 'Asignar roles'                       , 640 , 320 , 220 , 70  , 0.00, 10, NULL, NULL),
  -- Diagrama 3: Gestion de Multimedia
  (15, 3, NULL, 'ACTOR'           , 'Usuario'                             , 40  , 240 , 80  , 140 , 0.00, 10, NULL, NULL),
  (16, 3, NULL, 'LIMITE_SISTEMA'  , 'Modulo Multimedia'                   , 180 , 80  , 900 , 520 , 0.00, 1 , NULL, NULL),
  (17, 3, NULL, 'CASO_DE_USO'     , 'Subir archivo multimedia'            , 260 , 140 , 260 , 70  , 0.00, 10, NULL, NULL),
  (18, 3, NULL, 'CASO_DE_USO'     , 'Validar archivo'                     , 580 , 140 , 220 , 70  , 0.00, 10, NULL, NULL),
  (19, 3, NULL, 'CASO_DE_USO'     , 'Asociar archivo a diagrama'          , 260 , 260 , 260 , 70  , 0.00, 10, NULL, NULL),
  (20, 3, NULL, 'CASO_DE_USO'     , 'Previsualizar archivo'               , 260 , 380 , 240 , 70  , 0.00, 10, NULL, NULL),
  (21, 3, NULL, 'CASO_DE_USO'     , 'Eliminar archivo'                    , 580 , 260 , 220 , 70  , 0.00, 10, NULL, NULL),
  (22, 3, NULL, 'NOTA'            , 'Formatos permitidos: MP3, MP4, JPG'  , 560 , 360 , 300 , 120 , 0.00, 10, NULL, NULL);

INSERT INTO `conexiones_diagrama` (
  `id_conexion`, `id_diagrama`, `id_elemento_origen`, `id_elemento_destino`,
  `tipo_conexion`, `etiqueta`, `puntos_json`, `estilo_json`
) VALUES
  -- Diagrama 1: Gestion de Diagramas
  (1 , 1, 1 , 3 , 'ASOCIACION'  , NULL          , NULL, NULL),
  (2 , 1, 1 , 4 , 'ASOCIACION'  , NULL          , NULL, NULL),
  (3 , 1, 1 , 5 , 'ASOCIACION'  , NULL          , NULL, NULL),
  (4 , 1, 1 , 6 , 'ASOCIACION'  , NULL          , NULL, NULL),
  (5 , 1, 4 , 7 , 'INCLUSION'   , '<<include>>' , NULL, NULL),
  -- Diagrama 2: Autenticacion y Usuarios
  (6 , 2, 8 , 11, 'ASOCIACION'  , NULL          , NULL, NULL),
  (7 , 2, 8 , 12, 'ASOCIACION'  , NULL          , NULL, NULL),
  (8 , 2, 9 , 13, 'ASOCIACION'  , NULL          , NULL, NULL),
  (9 , 2, 9 , 14, 'ASOCIACION'  , NULL          , NULL, NULL),
  (10, 2, 13, 14, 'INCLUSION'   , '<<include>>' , NULL, NULL),
  -- Diagrama 3: Gestion de Multimedia
  (11, 3, 15, 17, 'ASOCIACION'  , NULL          , NULL, NULL),
  (12, 3, 15, 19, 'ASOCIACION'  , NULL          , NULL, NULL),
  (13, 3, 15, 20, 'ASOCIACION'  , NULL          , NULL, NULL),
  (14, 3, 15, 21, 'ASOCIACION'  , NULL          , NULL, NULL),
  (15, 3, 17, 18, 'INCLUSION'   , '<<include>>' , NULL, NULL),
  (16, 3, 22, 17, 'ENLACE_NOTA' , NULL          , NULL, NULL);

SET FOREIGN_KEY_CHECKS=1;