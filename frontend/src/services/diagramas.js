import { del, get, post, put, upload } from './api.js';

export function listarDiagramas() {
  return get('/api/diagramas');
}

export function obtenerDiagrama(id_diagrama) {
  return get(`/api/diagramas?id_diagrama=${id_diagrama}`);
}

export function crearDiagrama(payload) {
  return post('/api/diagramas', payload);
}

export function actualizarDiagrama(payload) {
  return put('/api/diagramas', payload);
}

export function eliminarDiagrama(id_diagrama) {
  return del(`/api/diagramas?id_diagrama=${id_diagrama}`);
}

export function listarElementos(id_diagrama) {
  return get(`/api/elementos?id_diagrama=${id_diagrama}`);
}

export function crearElemento(payload) {
  return post('/api/elementos', payload);
}

export function actualizarElemento(payload) {
  return put('/api/elementos', payload);
}

export function eliminarElemento(id_elemento) {
  return del(`/api/elementos?id_elemento=${id_elemento}`);
}

export function listarConexiones(id_diagrama) {
  return get(`/api/conexiones?id_diagrama=${id_diagrama}`);
}

export function crearConexion(payload) {
  return post('/api/conexiones', payload);
}

export function actualizarConexion(payload) {
  return put('/api/conexiones', payload);
}

export function eliminarConexion(id_conexion) {
  return del(`/api/conexiones?id_conexion=${id_conexion}`);
}

export function subirArchivo(formData) {
  return upload('/api/archivos', formData);
}

export function listarArchivos() {
  return get('/api/archivos');
}

export function obtenerArchivo(id_archivo) {
  return get(`/api/archivos?id_archivo=${id_archivo}`);
}

export function eliminarArchivo(id_archivo) {
  return del(`/api/archivos?id_archivo=${id_archivo}`);
}

export function listarDiagramaMultimedia(id_diagrama) {
  return get(`/api/diagrama-multimedia?id_diagrama=${id_diagrama}`);
}

export function agregarDiagramaMultimedia(payload) {
  return post('/api/diagrama-multimedia', payload);
}

export function eliminarDiagramaMultimedia(id_diagrama, id_archivo) {
  return del(`/api/diagrama-multimedia?id_diagrama=${id_diagrama}&id_archivo=${id_archivo}`);
}

export function listarElementoMultimedia(id_elemento) {
  return get(`/api/elemento-multimedia?id_elemento=${id_elemento}`);
}

export function agregarElementoMultimedia(payload) {
  return post('/api/elemento-multimedia', payload);
}

export function eliminarElementoMultimedia(id_elemento, id_archivo) {
  return del(`/api/elemento-multimedia?id_elemento=${id_elemento}&id_archivo=${id_archivo}`);
}
