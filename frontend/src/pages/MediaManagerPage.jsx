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

export default function MediaManagerPage() {
  const { id } = useParams();
  const [multimedia, setMultimedia] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const cargar = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await listarDiagramaMultimedia(id);
      setMultimedia(data.multimedia || []);
    } catch (err) {
      setError(err?.data?.mensaje || 'No se pudo cargar el contenido multimedia.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    cargar();
  }, [id]);

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
    const tipoMedia = obtenerTipoMedia(file);
    try {
      const formData = new FormData();
      formData.append('archivo', file);
      formData.append('tipo_media', tipoMedia);
      formData.append('titulo', file.name);
      const result = await subirArchivo(formData);
      if (result?.id_archivo) {
        await agregarDiagramaMultimedia({
          id_diagrama: Number(id),
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

  const handleDrop = (event) => {
    event.preventDefault();
    handleFiles(event.dataTransfer.files);
  };

  const handleDelete = async (idArchivo) => {
    if (!window.confirm('Eliminar esta asociacion?')) {
      return;
    }
    try {
      await eliminarDiagramaMultimedia(id, idArchivo);
      await cargar();
    } catch (err) {
      setError(err?.data?.mensaje || 'No se pudo eliminar la asociacion.');
    }
  };

  return (
    <div className="row g-4">
      <div className="col-12">
        <Link to={`/diagramas/${id}`} className="btn btn-outline-secondary btn-sm">
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
