# Casos

Proyecto academico para una aplicacion web graficadora de diagramas de casos de uso UML con soporte multimedia (MP3, MP4, JPG). El frontend esta hecho en React y el backend en Java Servlets con MySQL.

## Contexto y flujo general
- La base de datos se define en `frontend/Base.sql` y se llama `aplicacion`.
- El frontend se construye con Webpack y el `dist/` resultante se copia a `backend/src/main/webapp/` para servir todo desde Tomcat (flujo academico).
- El backend expone endpoints REST en `/api/*` y mantiene un endpoint legacy `/Login`.

## Requisitos
- MySQL 8.x (se usan columnas JSON y CHECK).
- JDK compatible con el proyecto (ver `backend/pom.xml`).
- Tomcat (NetBeans lo integra para ejecucion local).

## Base de datos
Archivo: `frontend/Base.sql`

Tablas principales (nombres exactos):
- `roles` (`id_rol`, `nombre_rol`, `descripcion`)
- `usuarios` (`id_usuario`, `nombre_usuario`, `correo`, `contrasena`, `id_rol`, `fecha_creacion`, `fecha_actualizacion`)
- `diagramas_uml` (`id_diagrama`, `id_usuario`, `nombre`, `descripcion`, `estado`, `ancho_lienzo`, `alto_lienzo`, `configuracion_json`, fechas)
- `elementos_diagrama` (elementos graficos por diagrama)
- `conexiones_diagrama` (relaciones entre elementos)
- `archivos_multimedia` (metadatos y `ruta_archivo` relativa)
- `diagrama_multimedia` (asociacion diagrama-archivo)
- `elemento_multimedia` (asociacion elemento-archivo)

Datos iniciales:
- Rol administrador (`id_rol = 1`) y usuario base (`nombre_usuario = ADMINISTRADOR`, `contrasena = 1234`).

## Backend (Java Servlets)
Utilidades:
- `API/DB.java` lee DB desde variables de entorno o propiedades:
  - `DB_URL` / `db.url`
  - `DB_USER` / `db.user`
  - `DB_PASS` / `db.pass`
- `API/DbUtil.java` maneja la conexion JDBC.
- `API/JsonUtil.java` y `API/ResponseUtil.java` facilitan JSON y errores.
- `API/AuthFilter.java` protege `/api/*` (excepto `/api/auth/login`).

Autenticacion:
- `POST /api/auth/login` (body JSON o form):
  - `{ "nombre_usuario": "...", "contrasena": "..." }`
- `POST /api/auth/logout`
- Legacy: `GET /Login?User=...&password=...`

CRUD:
- `GET|POST|PUT|DELETE /api/usuarios` (admin para listar/crear/eliminar)
- `GET /api/roles`
- `GET|POST|PUT|DELETE /api/diagramas`
- `GET|POST|PUT|DELETE /api/elementos`
- `GET|POST|PUT|DELETE /api/conexiones`
- `GET|POST|DELETE /api/archivos` (subida con multipart)
- `GET|POST|DELETE /api/diagrama-multimedia`
- `GET|POST|DELETE /api/elemento-multimedia`

Notas:
- Los endpoints `/api/*` requieren sesion activa (cookie de sesion).
- Los enums enviados deben coincidir con los valores del esquema (`ACTIVO`, `ACTOR`, `ASOCIACION`, etc.).
- El backend usa `prepared statements` para evitar inyecciones basicas.

## Multimedia
- Los archivos se guardan en `uploads/` dentro del backend (ruta real del servlet).
- En DB se guarda `ruta_archivo` relativa con la extension incluida (ej: `uploads/uuid.mp3`).
- Los formatos permitidos son MP3, MP4 y JPG/JPEG, validados por extension.

## Frontend
Entrada principal:
- `frontend/index.js` monta la app React.
- Rutas basicas: `/login`, `/diagramas`, `/diagramas/:id`, `/diagramas/:id/multimedia`.

Build:
1. Instalar dependencias: `npm install` en `frontend/`.
2. Construir: `npm run build`.
3. Copiar `frontend/dist/*` a `backend/src/main/webapp/`.

## Ejecucion local
1. Crear BD: `mysql -u root -p < frontend/Base.sql`.
2. Configurar credenciales en `DB` o variables de entorno.
3. Desplegar el backend en Tomcat desde NetBeans.
4. Servir el frontend desde el mismo Tomcat (copiando `dist`).

## Pendientes comunes
- Ajustar `window.BACKEND_URL` si el backend corre en un host/puerto distinto al frontend.
- Implementar validaciones adicionales de negocio (si se requiere).
