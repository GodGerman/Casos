// Limite maximo para archivos multimedia en MB.
// Se usa para validar uploads en cliente antes de enviar al backend.
const MAX_MB = 20;

/**
 * Valida datos minimos de un diagrama.
 *
 * Se valida el nombre, las dimensiones minimas y el JSON opcional.
 *
 *
 * @param {object} diagrama datos a validar.
 * @returns {string[]} lista de errores; vacia si es valido.
 */
export function validarDiagrama(diagrama) {
  const errores = [];
  if (!diagrama.nombre || !diagrama.nombre.trim()) {
    errores.push('El nombre es obligatorio.');
  }
  if (!diagrama.ancho_lienzo || Number(diagrama.ancho_lienzo) < 300) {
    errores.push('El ancho del lienzo es invalido.');
  }
  if (!diagrama.alto_lienzo || Number(diagrama.alto_lienzo) < 300) {
    errores.push('El alto del lienzo es invalido.');
  }
  const errorConfig = validarJsonOpcional(diagrama.configuracion_json, 'configuracion_json');
  if (errorConfig) {
    errores.push(errorConfig);
  }
  return errores;
}

/**
 * Valida campos basicos de un elemento UML.
 *
 * Se valida el tipo, las dimensiones positivas y el JSON opcional.
 *
 *
 * @param {object} elemento datos a validar.
 * @returns {string[]} lista de errores; vacia si es valido.
 */
export function validarElemento(elemento) {
  const errores = [];
  if (!elemento.tipo_elemento) {
    errores.push('El tipo de elemento es obligatorio.');
  }
  if (elemento.ancho !== undefined && Number(elemento.ancho) <= 0) {
    errores.push('El ancho del elemento es invalido.');
  }
  if (elemento.alto !== undefined && Number(elemento.alto) <= 0) {
    errores.push('El alto del elemento es invalido.');
  }
  const errorEstilo = validarJsonOpcional(elemento.estilo_json, 'estilo_json');
  if (errorEstilo) {
    errores.push(errorEstilo);
  }
  const errorMeta = validarJsonOpcional(elemento.metadatos_json, 'metadatos_json');
  if (errorMeta) {
    errores.push(errorMeta);
  }
  return errores;
}

/**
 * Valida campos basicos de una conexion UML.
 *
 * Se valida el tipo y el JSON opcional de puntos/estilo.
 *
 *
 * @param {object} conexion datos a validar.
 * @returns {string[]} lista de errores; vacia si es valido.
 */
export function validarConexion(conexion) {
  const errores = [];
  if (!conexion.tipo_conexion) {
    errores.push('El tipo de conexion es obligatorio.');
  }
  const errorPuntos = validarJsonOpcional(conexion.puntos_json, 'puntos_json');
  if (errorPuntos) {
    errores.push(errorPuntos);
  }
  const errorEstilo = validarJsonOpcional(conexion.estilo_json, 'estilo_json');
  if (errorEstilo) {
    errores.push(errorEstilo);
  }
  return errores;
}

/**
 * Valida extension y tamano de un archivo multimedia.
 *
 * Se valida la extension contra la lista permitida y se compara contra MAX_MB.
 *
 *
 * @param {File} file archivo a validar.
 * @returns {{ok: boolean, mensaje?: string}} resultado de validacion.
 */
export function validarArchivo(file) {
  if (!file) {
    return { ok: false, mensaje: 'Selecciona un archivo.' };
  }
  const extension = file.name.split('.').pop().toLowerCase();
  const tipos = ['mp3', 'mp4', 'jpg', 'jpeg'];
  if (!tipos.includes(extension)) {
    return { ok: false, mensaje: 'Formato invalido. Usa MP3, MP4 o JPG.' };
  }
  const maxBytes = MAX_MB * 1024 * 1024;
  if (file.size > maxBytes) {
    return { ok: false, mensaje: `El archivo supera ${MAX_MB}MB.` };
  }
  return { ok: true };
}

/**
 * Valida un string JSON opcional, retornando error legible si falla.
 *
 * Se intenta JSON.parse y captura errores de sintaxis.
 *
 *
 * @param {string|null|undefined} valor string JSON.
 * @param {string} nombre nombre del campo para el mensaje.
 * @returns {string|null} mensaje de error o null si es valido.
 */
export function validarJsonOpcional(valor, nombre) {
  if (!valor || typeof valor !== 'string' || !valor.trim()) {
    return null;
  }
  try {
    JSON.parse(valor);
  } catch (err) {
    return `El campo ${nombre} debe ser JSON valido.`;
  }
  return null;
}

/**
 * Infere el tipo de media desde la extension del archivo.
 *
 * Se compara extension contra un mapa de tipos conocidos.
 *
 *
 * @param {File} file archivo a evaluar.
 * @returns {string|null} tipo (AUDIO/VIDEO/IMAGEN) o null si no aplica.
 */
export function obtenerTipoMedia(file) {
  if (!file) {
    return null;
  }
  const extension = file.name.split('.').pop().toLowerCase();
  if (extension === 'mp3') {
    return 'AUDIO';
  }
  if (extension === 'mp4') {
    return 'VIDEO';
  }
  if (extension === 'jpg' || extension === 'jpeg') {
    return 'IMAGEN';
  }
  return null;
}
