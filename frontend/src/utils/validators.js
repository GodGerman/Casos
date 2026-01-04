const MAX_MB = 20;

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
