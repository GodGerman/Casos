import { del, get, post, put, upload } from './api.js';

export function listarDiagramas() {
  return get('/api/diagramas');
}

export function obtenerDiagrama(idDiagrama) {
  return get(`/api/diagramas?id_diagrama=${idDiagrama}`);
}

export function crearDiagrama(payload) {
  return post('/api/diagramas', payload);
}

export function actualizarDiagrama(payload) {
  return put('/api/diagramas', payload);
}

export function eliminarDiagrama(idDiagrama) {
  return del(`/api/diagramas?id_diagrama=${idDiagrama}`);
}

export function listarElementos(idDiagrama) {
  return get(`/api/elementos?id_diagrama=${idDiagrama}`);
}

export function crearElemento(payload) {
  return post('/api/elementos', payload);
}

export function actualizarElemento(payload) {
  return put('/api/elementos', payload);
}

export function eliminarElemento(idElemento) {
  return del(`/api/elementos?id_elemento=${idElemento}`);
}

export function listarConexiones(idDiagrama) {
  return get(`/api/conexiones?id_diagrama=${idDiagrama}`);
}

export function crearConexion(payload) {
  return post('/api/conexiones', payload);
}

export function actualizarConexion(payload) {
  return put('/api/conexiones', payload);
}

export function eliminarConexion(idConexion) {
  return del(`/api/conexiones?id_conexion=${idConexion}`);
}

export function subirArchivo(formData) {
  return upload('/api/archivos', formData);
}

export function listarArchivos() {
  return get('/api/archivos');
}

export function obtenerArchivo(idArchivo) {
  return get(`/api/archivos?id_archivo=${idArchivo}`);
}

export function eliminarArchivo(idArchivo) {
  return del(`/api/archivos?id_archivo=${idArchivo}`);
}

export function listarDiagramaMultimedia(idDiagrama) {
  return get(`/api/diagrama-multimedia?id_diagrama=${idDiagrama}`);
}

export function agregarDiagramaMultimedia(payload) {
  return post('/api/diagrama-multimedia', payload);
}

export function eliminarDiagramaMultimedia(idDiagrama, idArchivo) {
  return del(`/api/diagrama-multimedia?id_diagrama=${idDiagrama}&id_archivo=${idArchivo}`);
}

export function listarElementoMultimedia(idElemento) {
  return get(`/api/elemento-multimedia?id_elemento=${idElemento}`);
}

export function agregarElementoMultimedia(payload) {
  return post('/api/elemento-multimedia', payload);
}

export function eliminarElementoMultimedia(idElemento, idArchivo) {
  return del(`/api/elemento-multimedia?id_elemento=${idElemento}&id_archivo=${idArchivo}`);
}
