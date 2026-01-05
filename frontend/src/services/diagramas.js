import { del, get, post, put, upload } from './api.js';

/**
 * Lista diagramas del usuario autenticado (o todos si admin).
 *
 * Se realiza una llamada GET al endpoint /api/diagramas.
 *
 *
 * @returns {Promise<object>} respuesta con arreglo de diagramas.
 * @throws {Error} si la respuesta no es ok.
 */
export function listarDiagramas() {
  return get('/api/diagramas');
}

/**
 * Obtiene un diagrama por id.
 *
 * Se envia el id como query string al endpoint /api/diagramas.
 *
 *
 * @param {number|string} id_diagrama id del diagrama.
 * @returns {Promise<object>} respuesta con diagrama.
 * @throws {Error} si la respuesta no es ok.
 */
export function obtenerDiagrama(id_diagrama) {
  return get(`/api/diagramas?id_diagrama=${id_diagrama}`);
}

/**
 * Crea un diagrama nuevo.
 *
 * Se envia el payload en POST a /api/diagramas.
 *
 *
 * @param {object} payload datos del diagrama.
 * @returns {Promise<object>} respuesta con id generado.
 * @throws {Error} si la respuesta no es ok.
 */
export function crearDiagrama(payload) {
  return post('/api/diagramas', payload);
}

/**
 * Actualiza un diagrama existente.
 *
 * Se envia el payload en PUT a /api/diagramas.
 *
 *
 * @param {object} payload datos del diagrama.
 * @returns {Promise<object>} respuesta del backend.
 * @throws {Error} si la respuesta no es ok.
 */
export function actualizarDiagrama(payload) {
  return put('/api/diagramas', payload);
}

/**
 * Elimina un diagrama por id.
 *
 * Se envia el id como query string a /api/diagramas con DELETE.
 *
 *
 * @param {number|string} id_diagrama id del diagrama.
 * @returns {Promise<object>} respuesta del backend.
 * @throws {Error} si la respuesta no es ok.
 */
export function eliminarDiagrama(id_diagrama) {
  return del(`/api/diagramas?id_diagrama=${id_diagrama}`);
}

/**
 * Lista elementos de un diagrama.
 *
 * Se envia el id_diagrama como query string a /api/elementos.
 *
 *
 * @param {number|string} id_diagrama id del diagrama.
 * @returns {Promise<object>} respuesta con elementos.
 * @throws {Error} si la respuesta no es ok.
 */
export function listarElementos(id_diagrama) {
  return get(`/api/elementos?id_diagrama=${id_diagrama}`);
}

/**
 * Crea un elemento dentro de un diagrama.
 *
 * Se envia el payload en POST a /api/elementos.
 *
 *
 * @param {object} payload datos del elemento.
 * @returns {Promise<object>} respuesta con id generado.
 * @throws {Error} si la respuesta no es ok.
 */
export function crearElemento(payload) {
  return post('/api/elementos', payload);
}

/**
 * Actualiza un elemento existente.
 *
 * Se envia el payload en PUT a /api/elementos.
 *
 *
 * @param {object} payload datos del elemento.
 * @returns {Promise<object>} respuesta del backend.
 * @throws {Error} si la respuesta no es ok.
 */
export function actualizarElemento(payload) {
  return put('/api/elementos', payload);
}

/**
 * Elimina un elemento por id.
 *
 * Se envia el id_elemento como query string a /api/elementos con DELETE.
 *
 *
 * @param {number|string} id_elemento id del elemento.
 * @returns {Promise<object>} respuesta del backend.
 * @throws {Error} si la respuesta no es ok.
 */
export function eliminarElemento(id_elemento) {
  return del(`/api/elementos?id_elemento=${id_elemento}`);
}

/**
 * Lista conexiones de un diagrama.
 *
 * Se envia el id_diagrama como query string a /api/conexiones.
 *
 *
 * @param {number|string} id_diagrama id del diagrama.
 * @returns {Promise<object>} respuesta con conexiones.
 * @throws {Error} si la respuesta no es ok.
 */
export function listarConexiones(id_diagrama) {
  return get(`/api/conexiones?id_diagrama=${id_diagrama}`);
}

/**
 * Crea una conexion entre elementos.
 *
 * Se envia el payload en POST a /api/conexiones.
 *
 *
 * @param {object} payload datos de la conexion.
 * @returns {Promise<object>} respuesta con id generado.
 * @throws {Error} si la respuesta no es ok.
 */
export function crearConexion(payload) {
  return post('/api/conexiones', payload);
}

/**
 * Actualiza una conexion existente.
 *
 * Se envia el payload en PUT a /api/conexiones.
 *
 *
 * @param {object} payload datos de la conexion.
 * @returns {Promise<object>} respuesta del backend.
 * @throws {Error} si la respuesta no es ok.
 */
export function actualizarConexion(payload) {
  return put('/api/conexiones', payload);
}

/**
 * Elimina una conexion por id.
 *
 * Se envia el id_conexion como query string a /api/conexiones con DELETE.
 *
 *
 * @param {number|string} id_conexion id de la conexion.
 * @returns {Promise<object>} respuesta del backend.
 * @throws {Error} si la respuesta no es ok.
 */
export function eliminarConexion(id_conexion) {
  return del(`/api/conexiones?id_conexion=${id_conexion}`);
}

/**
 * Sube un archivo multimedia (multipart/form-data).
 *
 * Se delega en upload para enviar FormData a /api/archivos.
 *
 *
 * @param {FormData} formData payload multipart.
 * @returns {Promise<object>} respuesta con id_archivo y ruta.
 * @throws {Error} si la respuesta no es ok.
 */
export function subirArchivo(formData) {
  return upload('/api/archivos', formData);
}

/**
 * Lista archivos multimedia del usuario.
 *
 * Se realiza GET a /api/archivos sin parametros.
 *
 *
 * @returns {Promise<object>} respuesta con archivos.
 * @throws {Error} si la respuesta no es ok.
 */
export function listarArchivos() {
  return get('/api/archivos');
}

/**
 * Obtiene un archivo por id.
 *
 * Se envia el id_archivo como query string a /api/archivos.
 *
 *
 * @param {number|string} id_archivo id del archivo.
 * @returns {Promise<object>} respuesta con archivo.
 * @throws {Error} si la respuesta no es ok.
 */
export function obtenerArchivo(id_archivo) {
  return get(`/api/archivos?id_archivo=${id_archivo}`);
}

/**
 * Elimina un archivo por id.
 *
 * Se envia el id_archivo como query string a /api/archivos con DELETE.
 *
 *
 * @param {number|string} id_archivo id del archivo.
 * @returns {Promise<object>} respuesta del backend.
 * @throws {Error} si la respuesta no es ok.
 */
export function eliminarArchivo(id_archivo) {
  return del(`/api/archivos?id_archivo=${id_archivo}`);
}

/**
 * Lista multimedia asociada a un diagrama.
 *
 * Se envia el id_diagrama como query string a /api/diagrama-multimedia.
 *
 *
 * @param {number|string} id_diagrama id del diagrama.
 * @returns {Promise<object>} respuesta con multimedia.
 * @throws {Error} si la respuesta no es ok.
 */
export function listarDiagramaMultimedia(id_diagrama) {
  return get(`/api/diagrama-multimedia?id_diagrama=${id_diagrama}`);
}

/**
 * Asocia un archivo a un diagrama.
 *
 * Se envia el payload en POST a /api/diagrama-multimedia.
 *
 *
 * @param {object} payload datos de asociacion.
 * @returns {Promise<object>} respuesta del backend.
 * @throws {Error} si la respuesta no es ok.
 */
export function agregarDiagramaMultimedia(payload) {
  return post('/api/diagrama-multimedia', payload);
}

/**
 * Elimina la asociacion diagrama-archivo.
 *
 * Se envia ambos ids como query string a /api/diagrama-multimedia con DELETE.
 *
 *
 * @param {number|string} id_diagrama id del diagrama.
 * @param {number|string} id_archivo id del archivo.
 * @returns {Promise<object>} respuesta del backend.
 * @throws {Error} si la respuesta no es ok.
 */
export function eliminarDiagramaMultimedia(id_diagrama, id_archivo) {
  return del(`/api/diagrama-multimedia?id_diagrama=${id_diagrama}&id_archivo=${id_archivo}`);
}

/**
 * Lista multimedia asociada a un elemento.
 *
 * Se envia el id_elemento como query string a /api/elemento-multimedia.
 *
 *
 * @param {number|string} id_elemento id del elemento.
 * @returns {Promise<object>} respuesta con multimedia.
 * @throws {Error} si la respuesta no es ok.
 */
export function listarElementoMultimedia(id_elemento) {
  return get(`/api/elemento-multimedia?id_elemento=${id_elemento}`);
}

/**
 * Asocia un archivo a un elemento.
 *
 * Se envia el payload en POST a /api/elemento-multimedia.
 *
 *
 * @param {object} payload datos de asociacion.
 * @returns {Promise<object>} respuesta del backend.
 * @throws {Error} si la respuesta no es ok.
 */
export function agregarElementoMultimedia(payload) {
  return post('/api/elemento-multimedia', payload);
}

/**
 * Elimina la asociacion elemento-archivo.
 *
 * Se envia ambos ids como query string a /api/elemento-multimedia con DELETE.
 *
 *
 * @param {number|string} id_elemento id del elemento.
 * @param {number|string} id_archivo id del archivo.
 * @returns {Promise<object>} respuesta del backend.
 * @throws {Error} si la respuesta no es ok.
 */
export function eliminarElementoMultimedia(id_elemento, id_archivo) {
  return del(`/api/elemento-multimedia?id_elemento=${id_elemento}&id_archivo=${id_archivo}`);
}
