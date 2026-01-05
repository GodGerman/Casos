import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import AlertMessage from '../components/AlertMessage.jsx';
import Loading from '../components/Loading.jsx';
import {
  agregarDiagramaMultimedia,
  eliminarDiagramaMultimedia,
  listarDiagramaMultimedia,
  subirArchivo
} from '../services/diagramas.js';
import { buildFileUrl } from '../services/api.js';
import { obtenerTipoMedia, validarArchivo } from '../utils/validators.js';

/**
 * Gestor de multimedia asociada a un diagrama.
 *
 * Se lista archivos asociados, permite subir nuevos
 * y crear la relacion diagrama-archivo en el backend.
 *
 *
 * @returns {JSX.Element} pagina de carga y listado multimedia.
 */
export default function MediaManagerPage() {
  const { id: id_diagrama } = useParams();
  const [multimedia, setMultimedia] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  /**
   * Carga la lista de multimedia asociada al diagrama.
   *
   * @returns {Promise<void>} no retorna valor; actualiza estado local.
   * Si falla la red o el backend, actualiza el mensaje de error.
   *
   * Se llama al endpoint de listado y guarda el arreglo en estado.
   *
   */
  const cargar = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await listarDiagramaMultimedia(id_diagrama);
      setMultimedia(data.multimedia || []);
    } catch (err) {
      setError(err?.data?.mensaje || 'No se pudo cargar el contenido multimedia.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    cargar();
  }, [id_diagrama]);

  /**
   * Valida y sube un archivo, luego lo asocia al diagrama.
   *
   * @param {FileList} files archivos seleccionados o arrastrados.
   * @returns {Promise<void>} no retorna valor; actualiza listado.
   * Si el backend falla, actualiza el mensaje de error.
   *
   * Se valida la extension y el tamano, se sube via multipart y
   * se crea la relacion en diagrama_multimedia.
   *
   */
  const handleFiles = async (files) => {
    if (!files || files.length === 0) {
      return;
    }
    setError('');
    setSuccess('');
    const file = files[0];
    const validacion = validarArchivo(file);
    if (!validacion.ok) {
      setError(validacion.mensaje);
      return;
    }
    const tipo_media = obtenerTipoMedia(file);
    try {
      const formData = new FormData();
      formData.append('archivo', file);
      formData.append('tipo_media', tipo_media);
      formData.append('titulo', file.name);
      const result = await subirArchivo(formData);
      if (result?.id_archivo) {
        await agregarDiagramaMultimedia({
          id_diagrama: Number(id_diagrama),
          id_archivo: result.id_archivo,
          descripcion: null,
          orden: 0
        });
        setSuccess('Archivo subido y asociado.');
        await cargar();
      }
    } catch (err) {
      setError(err?.data?.mensaje || 'Error al subir el archivo.');
    }
  };

  /**
   * Handler de drop para arrastrar archivos al area de carga.
   *
   * @param {DragEvent} event evento de drop.
   * @returns {void} no retorna valor; delega a handleFiles.
   *
   * Se cancela el comportamiento por defecto y toma los archivos
   * desde dataTransfer.
   *
   */
  const handleDrop = (event) => {
    event.preventDefault();
    handleFiles(event.dataTransfer.files);
  };

  /**
   * Elimina la asociacion entre el diagrama y un archivo.
   *
   * @param {number} id_archivo id del archivo a quitar.
   * @returns {Promise<void>} no retorna valor; recarga listado.
   * Si el backend falla, actualiza el mensaje de error.
   *
   * Se confirma con el usuario, llama al endpoint de borrado
   * de la relacion y recarga el listado.
   *
   */
  const handleDelete = async (id_archivo) => {
    if (!window.confirm('Eliminar esta asociacion?')) {
      return;
    }
    try {
      await eliminarDiagramaMultimedia(id_diagrama, id_archivo);
      await cargar();
    } catch (err) {
      setError(err?.data?.mensaje || 'No se pudo eliminar la asociacion.');
    }
  };

  return (
    <div className="row g-4">
      <div className="col-12">
        <Link to={`/diagramas/${id_diagrama}`} className="btn btn-outline-secondary btn-sm">
          Volver al editor
        </Link>
      </div>
      <div className="col-12">
        <AlertMessage type="danger" message={error} />
        <AlertMessage type="success" message={success} />
      </div>
      <div className="col-lg-4">
        <div
          className="card shadow-sm drop-zone"
          onDragOver={(event) => event.preventDefault()}
          onDrop={handleDrop}
        >
          <div className="card-body text-center">
            <h5>Subir multimedia</h5>
            <p className="text-muted">Arrastra archivos MP3, MP4 o JPG aqui.</p>
            <input
              className="form-control"
              type="file"
              accept=".mp3,.mp4,.jpg,.jpeg"
              onChange={(event) => handleFiles(event.target.files)}
            />
          </div>
        </div>
      </div>
      <div className="col-lg-8">
        <div className="card shadow-sm">
          <div className="card-header">Multimedia del diagrama</div>
          <div className="card-body">
            {loading && <Loading text="Cargando multimedia..." />}
            {!loading && multimedia.length === 0 && (
              <p className="text-muted">No hay archivos asociados.</p>
            )}
            {!loading && multimedia.length > 0 && (
              <div className="row g-3">
                {multimedia.map((item) => {
                  const url = buildFileUrl(item.ruta_archivo);
                  return (
                    <div className="col-md-6" key={`${item.id_diagrama}-${item.id_archivo}`}>
                      <div className="card h-100">
                        <div className="card-body">
                          <h6 className="card-title">{item.titulo || item.tipo_media}</h6>
                          {item.tipo_media === 'IMAGEN' && (
                            <img className="img-fluid rounded" src={url} alt={item.titulo || 'imagen'} />
                          )}
                          {item.tipo_media === 'VIDEO' && (
                            <video className="w-100" controls src={url} />
                          )}
                          {item.tipo_media === 'AUDIO' && (
                            <audio className="w-100" controls src={url} />
                          )}
                        </div>
                        <div className="card-footer d-flex justify-content-between align-items-center">
                          <span className="text-muted small">#{item.id_archivo}</span>
                          <button
                            className="btn btn-sm btn-outline-danger"
                            type="button"
                            onClick={() => handleDelete(item.id_archivo)}
                          >
                            Quitar
                          </button>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
